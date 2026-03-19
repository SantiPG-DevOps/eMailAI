package com.emailAI.service;

import com.emailAI.ia.GestorModelos;
import com.emailAI.model.Mensaje;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

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
    private String imapHost = null; // recordar el host IMAP

    /**
     * Establece la conexión IMAP para recibir y prepara los datos SMTP para enviar.
     */
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
    }

    /**
     * Descarga los últimos mensajes y los clasifica mediante IA (Spam y Prioridad).
     * Guarda texto plano en Mensaje.cuerpo y HTML (si lo hay) en Mensaje.html.
     */
    public List<Mensaje> listInbox() throws Exception {
        List<Mensaje> resultado = new ArrayList<>();

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.getMessages();

        int total = messages.length;
        int max = 20; // último bloque de correos para procesar
        int inicio = Math.max(1, total - max + 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = total; i >= inicio; i--) { // Iteramos de más reciente a más antiguo
            Message msg = messages[i - 1];

            // Identificador lógico (de momento messageNumber)
            String id = String.valueOf(msg.getMessageNumber());

            Address[] froms = msg.getFrom();
            String remitente = (froms != null && froms.length > 0)
                    ? ((InternetAddress) froms[0]).getAddress()
                    : "desconocido";

            String asunto = msg.getSubject();

            // Texto plano (para BD + IA)
            String cuerpo = extraerCuerpoTexto(msg);

            // HTML (solo interfaz; si no hay, será null)
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

            // Guardamos el HTML solo para la UI
            mensajeObj.setHtml(html);

            // --- INTEGRACIÓN DE INTELIGENCIA ARTIFICIAL ---
            try {
                String categoria = GestorModelos.clasificarSpam(mensajeObj);
                mensajeObj.setCategoria(categoria);

                if (!"SPAM".equalsIgnoreCase(categoria)) {
                    String prioridad = GestorModelos.clasificarPrioridad(mensajeObj);
                    mensajeObj.setPrioridad(prioridad);
                } else {
                    mensajeObj.setPrioridad("NORMAL"); // El spam nunca es urgente
                }
            } catch (Exception e) {
                System.err.println("Error procesando IA para mensaje " + id + ": " + e.getMessage());
                mensajeObj.setCategoria("DESCONOCIDO");
                mensajeObj.setPrioridad("NORMAL");
            }

            resultado.add(mensajeObj);
        }

        inbox.close(false);
        return resultado;
    }

    /**
     * Envía un correo electrónico utilizando el servidor SMTP configurado.
     */
    public void sendEmail(String to, String subject, String body) throws Exception {
        if (session == null || currentUser == null || currentPassword == null) {
            throw new IllegalStateException("No hay sesión de correo iniciada.");
        }

        Properties props = new Properties();
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

    /**
     * Extrae contenido en texto plano del mensaje, manejando estructuras Multipart.
     * Este texto se usa para BD y modelo IA.
     */
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

    /**
     * Extrae el cuerpo HTML principal del mensaje, si existe.
     * Si no hay HTML, devuelve null.
     */
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
                    return (String) pc; // primer HTML que encontremos
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

    // ========== GETTERS PARA CorreoController ==========

    public String getEmail() {
        return currentUser;
    }

    public String getImapHost() {
        return imapHost;
    }
}
