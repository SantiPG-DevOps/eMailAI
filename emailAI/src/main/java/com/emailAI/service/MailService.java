package com.emailAI.service;

import com.emailAI.model.Mensaje;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.UIDFolder;
import weka.core.WekaException;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

// Encapsula la conexión IMAP/SMTP, la carga de mensajes y la integración con los servicios de IA.
public class MailService {

    /** Carpeta IMAP listada en el menú lateral (nombre completo + texto mostrado). */
    public record CarpetaSidebar(String imapFullName, String etiquetaVista) {
        @Override
        public String toString() {
            return etiquetaVista != null && !etiquetaVista.isBlank() ? etiquetaVista : imapFullName;
        }
    }

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

    // Establece la sesión IMAP y guarda credenciales y hosts para posteriores operaciones.
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

    // Obtiene los últimos N mensajes de la bandeja de entrada y les aplica clasificación IA.
    public List<Mensaje> listInbox() throws Exception {
        return listMensajesDeCarpeta("INBOX", 20);
    }

    /**
     * Lista carpetas que pueden contener mensajes (recursivo desde la raíz de la cuenta).
     */
    public List<CarpetaSidebar> listarCarpetasAccesibles() throws MessagingException {
        if (store == null || !store.isConnected()) {
            return List.of();
        }
        List<CarpetaSidebar> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Folder root = store.getDefaultFolder();
        recolectarCarpetas(root, out, seen);
        try {
            Folder inbox = store.getFolder("INBOX");
            if (inbox.exists()) {
                recolectarCarpetas(inbox, out, seen);
            }
        } catch (MessagingException ignored) {
        }
        out.sort(Comparator.comparing(CarpetaSidebar::imapFullName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private void recolectarCarpetas(Folder folder, List<CarpetaSidebar> out, Set<String> seen) throws MessagingException {
        if (folder == null) {
            return;
        }
        String key = nombreCarpetaNormalizado(folder);
        int type = folder.getType();
        boolean soportaMensajes = (type & Folder.HOLDS_MESSAGES) != 0;
        if (soportaMensajes && !seen.contains(key)) {
            seen.add(key);
            out.add(new CarpetaSidebar(key, etiquetaCarpetaParaUi(key)));
        }
        if ((type & Folder.HOLDS_FOLDERS) != 0) {
            Folder[] hijos = folder.list("%");
            if (hijos != null) {
                for (Folder h : hijos) {
                    recolectarCarpetas(h, out, seen);
                }
            }
        }
    }

    private static String nombreCarpetaNormalizado(Folder folder) throws MessagingException {
        String full = folder.getFullName();
        if (full != null && !full.isBlank()) {
            return full;
        }
        String name = folder.getName();
        return (name != null && !name.isBlank()) ? name : "INBOX";
    }

    private static String etiquetaCarpetaParaUi(String imapFullName) {
        if (imapFullName == null || imapFullName.isBlank()) {
            return "";
        }
        return imapFullName.replace("/", " › ");
    }

    /** Últimos {@code max} mensajes de una carpeta por su nombre IMAP completo. */
    public List<Mensaje> listMensajesDeCarpeta(String imapFullName, int max) throws Exception {
        if (store == null || !store.isConnected()) {
            throw new IllegalStateException("Store IMAP no conectado.");
        }
        Folder f = store.getFolder(imapFullName);
        if (!f.exists()) {
            throw new MessagingException("La carpeta no existe en el servidor: " + imapFullName);
        }
        f.open(Folder.READ_ONLY);
        try {
            // Misma clave que en el menú lateral y en SQLite (evita desajustes con getFullName()).
            return listarUltimosMensajesDeCarpeta(f, max, imapFullName);
        } finally {
            f.close(false);
        }
    }

    // Lee hasta {@code max} mensajes recientes de una carpeta ya abierta.
    private List<Mensaje> listarUltimosMensajesDeCarpeta(Folder folder, int max, String carpetaClaveBd) throws Exception {
        List<Mensaje> resultado = new ArrayList<>();
        Message[] messages = folder.getMessages();

        int total = messages.length;
        int inicio = Math.max(1, total - max + 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = total; i >= inicio; i--) {
            Message msg = messages[i - 1];
            String uid = obtenerIdentificadorMensaje(folder, msg);

            Address[] froms = msg.getFrom();
            String remitente = (froms != null && froms.length > 0)
                    ? ((InternetAddress) froms[0]).getAddress()
                    : "desconocido";

            String asunto = msg.getSubject();
            String cuerpo = extraerCuerpoTexto(msg);
            String html = extraerCuerpoHtml(msg);

            Mensaje mensajeObj = new Mensaje(uid, remitente, asunto, cuerpo);

            mensajeObj.setUidImap(uid);
            if (msg.getReceivedDate() != null) {
                mensajeObj.setFecha(sdf.format(msg.getReceivedDate()));
            } else if (msg.getSentDate() != null) {
                mensajeObj.setFecha(sdf.format(msg.getSentDate()));
            } else {
                mensajeObj.setFecha("");
            }

            mensajeObj.setHtml(html);
            mensajeObj.setCuentaHash(cuentaHash);
            mensajeObj.setCarpetaImap(carpetaClaveBd);

            // ========== IA clásica (Weka) ==========
            try {
                if (spamIaService != null) {
                    SpamIaService.ClaseCorreo clase = spamIaService.clasificar(cuentaHash, mensajeObj);
                    String categoria = clase.name(); // LEGITIMO / SPAM / PHISHING
                    mensajeObj.setCategoria(categoria);

                    if (!"SPAM".equalsIgnoreCase(categoria)) {
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
                System.err.println("Error procesando IA clásica para mensaje " + uid + ": " + e.getMessage());
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
                    System.err.println("Error llamando a IAAsistenteService para mensaje " + uid + ": " + e.getMessage());
                }
            }

            resultado.add(mensajeObj);
        }

        return resultado;
    }

    // Marca como eliminado un mensaje en la carpeta IMAP indicada.
    public void eliminarMensaje(Mensaje mensaje, String carpetaImapFullName) throws Exception {
        if (store == null || !store.isConnected()) {
            throw new IllegalStateException("Store IMAP no conectado.");
        }
        if (mensaje == null) return;

        String carpeta = carpetaImapFullName != null && !carpetaImapFullName.isBlank()
                ? carpetaImapFullName
                : "INBOX";

        Folder folder = store.getFolder(carpeta);
        if (!folder.exists()) {
            throw new IllegalStateException("Carpeta inexistente: " + carpeta);
        }
        folder.open(Folder.READ_WRITE);

        Message msg = obtenerMensajeParaBorrado(folder, mensaje.getUidImap());
        if (msg == null) {
            folder.close(false);
            throw new IllegalStateException("No se encontró el mensaje en " + carpeta + " para uid=" + mensaje.getUidImap());
        }

        msg.setFlag(Flags.Flag.DELETED, true);
        folder.close(true);
    }

    // ========================= Envío =========================

    // Envía un correo de texto plano usando SMTP autenticado con STARTTLS.
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

    // Cierra de forma segura la conexión IMAP si sigue abierta.
    public void close() throws Exception {
        if (store != null && store.isConnected()) {
            store.close();
        }
    }

    // ===================== helpers de extracción =====================

    // Extrae la representación de texto plano desde un mensaje potencialmente multiparte.
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

    // Intenta extraer una versión HTML del cuerpo del mensaje si existe.
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

    // Recorre recursivamente las partes de un multipart buscando contenido HTML.
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

    // Devuelve la dirección de correo del usuario autenticado.
    public String getEmail() {
        return currentUser;
    }

    // Devuelve el host IMAP actualmente configurado.
    public String getImapHost() {
        return imapHost;
    }
    
    // Devuelve el servicio de IA clásica asociado (puede ser null si falló la inicialización).
    public SpamIaService getSpamIaService() {
        return spamIaService;
    }

    // Obtiene un identificador estable de mensaje priorizando UID IMAP y usando messageNumber como respaldo.
    private String obtenerIdentificadorMensaje(Folder inbox, Message msg) throws MessagingException {
        if (inbox instanceof UIDFolder uidFolder) {
            long uid = uidFolder.getUID(msg);
            if (uid > 0) {
                return String.valueOf(uid);
            }
        }
        return String.valueOf(msg.getMessageNumber());
    }

    // Localiza el Message correspondiente a un UID dado, con fallback al índice numérico.
    private Message obtenerMensajeParaBorrado(Folder inbox, String uidImap) throws MessagingException {
        if (uidImap == null || uidImap.isBlank()) return null;

        try {
            long uid = Long.parseLong(uidImap);
            if (inbox instanceof UIDFolder uidFolder) {
                Message byUid = uidFolder.getMessageByUID(uid);
                if (byUid != null) return byUid;
            }
        } catch (NumberFormatException ignored) {
            // Compatibilidad con datos antiguos no numéricos.
        }

        try {
            int msgNumber = Integer.parseInt(uidImap);
            return inbox.getMessage(msgNumber);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}