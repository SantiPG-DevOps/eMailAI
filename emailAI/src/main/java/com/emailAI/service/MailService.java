package com.emailAI.service;

import com.emailAI.model.Mensaje;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import weka.core.WekaException;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailService {

    private Session session;
    private Store store;
    private String currentUser;
    private String currentPassword;
    private String smtpHost = null;
    private String imapHost = null;

    // identificador de cuenta para la BD (por ahora usamos el email)
    private String cuentaHash;

    // IA LLM opcional
    private IAAsistenteService iaAsistenteService;

    // IA clásica (Weka)
    private SpamIaService spamIaService;

    public MailService() {
    }

    public MailService(IAAsistenteService iaAsistenteService) {
        this.iaAsistenteService = iaAsistenteService;
    }

    public void setIaAsistenteService(IAAsistenteService iaAsistenteService) {
        this.iaAsistenteService = iaAsistenteService;
    }

    public String getCuentaHash() {
        return cuentaHash;
    }

    // ========================= Conexión =========================

    public void connect(String imapHost, String smtpHost, String user, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.host", imapHost);
        props.setProperty("mail.imap.port", "993");
        props.setProperty("mail.imap.ssl.enable", "true");

        session = Session.getInstance(props);
        store = session.getStore("imap");
        store.connect(imapHost, user, password);

        this.currentUser = user;
        this.currentPassword = password;
        this.smtpHost = smtpHost;
        this.imapHost = imapHost;

        // de momento usamos el email como hash de cuenta
        this.cuentaHash = user;

        // directorio para modelos por cuenta (carpeta "modelos" en el directorio de trabajo)
        Path modelosDir = Path.of("modelos");
        try {
            this.spamIaService = new SpamIaService(modelosDir);
        } catch (Exception e) {
            System.err.println("No se pudo inicializar SpamIaService: " + e.getMessage());
            this.spamIaService = null;
        }
    }

    // ========================= Bandeja de entrada =========================

    public List<Mensaje> listInbox() throws Exception {
        if (store == null || !store.isConnected()) {
            throw new IllegalStateException("Store IMAP no conectado. Llama antes a connect().");
        }

        List<Mensaje> resultado = new ArrayList<>();

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.getMessages();

        int total = messages.length;
        int max = 20;
        int inicio = Math.max(1, total - max + 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = total; i >= inicio; i--) {
            Message msg = messages[i - 1];

            String id = String.valueOf(msg.getMessageNumber());

            Address[] froms = msg.getFrom();
            String remitente = (froms != null && froms.length > 0)
                    ? ((InternetAddress) froms[0]).getAddress()
                    : "desconocido";

            String asunto = msg.getSubject();
            String cuerpo = extraerCuerpoTexto(msg);
            String html = extraerCuerpoHtml(msg);

            Mensaje mensajeObj = new Mensaje(id, remitente, asunto, cuerpo);

            mensajeObj.setUidImap(id);
            if (msg.getReceivedDate() != null) {
                mensajeObj.setFecha(sdf.format(msg.getReceivedDate()));
            } else if (msg.getSentDate() != null) {
                mensajeObj.setFecha(sdf.format(msg.getSentDate()));
            } else {
                mensajeObj.setFecha("");
            }

            mensajeObj.setHtml(html);
            mensajeObj.setCuentaHash(cuentaHash);

            // ========== IA clásica (Weka) ==========
            try {
                if (spamIaService != null) {
                    SpamIaService.ClaseCorreo clase = spamIaService.clasificar(cuentaHash, mensajeObj);
                    String categoria = clase.name(); // LEGITIMO / SPAM / PHISHING
                    mensajeObj.setCategoria(categoria);

                    if (!"SPAM".equalsIgnoreCase(categoria)) {
                        // de momento prioridad simple; luego afinamos
                        mensajeObj.setPrioridad("NORMAL");
                    } else {
                        mensajeObj.setPrioridad("NORMAL");
                    }
                } else {
                    mensajeObj.setCategoria("DESCONOCIDO");
                    mensajeObj.setPrioridad("NORMAL");
                }
            } catch (WekaException we) {
                System.err.println("Modelo Weka aún no entrenado, marcando como LEGITIMO por defecto.");
                mensajeObj.setCategoria("LEGITIMO");
                mensajeObj.setPrioridad("NORMAL");
            } catch (Exception e) {
                System.err.println("Error procesando IA clásica para mensaje " + id + ": " + e.getMessage());
                mensajeObj.setCategoria("DESCONOCIDO");
                mensajeObj.setPrioridad("NORMAL");
            }

            // ========== IA LLM opcional ==========
            if (iaAsistenteService != null) {
                try {
                    String resumen = iaAsistenteService.generarResumen(cuerpo);
                    String sugerencia = iaAsistenteService.sugerirRespuesta(cuerpo);
                    mensajeObj.setResumenIA(resumen);
                    mensajeObj.setSugerenciaIA(sugerencia);
                } catch (Exception e) {
                    System.err.println("Error llamando a IAAsistenteService para mensaje " + id + ": " + e.getMessage());
                }
            }

            resultado.add(mensajeObj);
        }

        inbox.close(false);
        return resultado;
    }

    public void eliminarMensaje(Mensaje mensaje) throws Exception {
        if (store == null || !store.isConnected()) {
            throw new IllegalStateException("Store IMAP no conectado.");
        }
        if (mensaje == null) return;

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        int msgNumber = Integer.parseInt(mensaje.getUidImap());
        Message msg = inbox.getMessage(msgNumber);

        msg.setFlag(Flags.Flag.DELETED, true);
        inbox.close(true);
    }

    // ========================= Envío =========================

    public void sendEmail(String to, String subject, String body) throws Exception {
        if (currentUser == null || currentPassword == null || smtpHost == null) {
            throw new IllegalStateException("No hay sesión SMTP/credenciales configuradas.");
        }

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", "587");

        Session smtpSession = Session.getInstance(props);
        Message message = new MimeMessage(smtpSession);
        message.setFrom(new InternetAddress(currentUser));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        try (Transport transport = smtpSession.getTransport("smtp")) {
            transport.connect(smtpHost, currentUser, currentPassword);
            transport.sendMessage(message, message.getAllRecipients());
        }
    }

    public void close() throws Exception {
        if (store != null && store.isConnected()) {
            store.close();
        }
    }

    // ===================== helpers de extracción =====================

    private String extraerCuerpoTexto(Message message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String s) {
            return s;
        } else if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String tipo = part.getContentType().toLowerCase();

                if (tipo.startsWith("text/plain")) {
                    sb.append(part.getContent().toString()).append("\n");
                } else if (part.getContent() instanceof Multipart innerMultipart) {
                    for (int j = 0; j < innerMultipart.getCount(); j++) {
                        BodyPart innerPart = innerMultipart.getBodyPart(j);
                        if (innerPart.getContentType().toLowerCase().startsWith("text/plain")) {
                            sb.append(innerPart.getContent().toString()).append("\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
        return "";
    }

    private String extraerCuerpoHtml(Message message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String s) {
            if (message.isMimeType("text/html")) {
                return s;
            } else {
                return null;
            }
        } else if (content instanceof Multipart multipart) {
            return extraerHtmlDeMultipart(multipart);
        }
        return null;
    }

    private String extraerHtmlDeMultipart(Multipart multipart) throws Exception {
        String html = null;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String tipo = part.getContentType().toLowerCase();

            if (tipo.startsWith("text/html")) {
                Object pc = part.getContent();
                if (pc instanceof String) {
                    return (String) pc;
                }
            } else if (part.getContent() instanceof Multipart inner) {
                String innerHtml = extraerHtmlDeMultipart(inner);
                if (innerHtml != null) {
                    html = innerHtml;
                }
            }
        }
        return html;
    }

    public String getEmail() {
        return currentUser;
    }

    public String getImapHost() {
        return imapHost;
    }
    
    public SpamIaService getSpamIaService() {
        return spamIaService;
    }
}