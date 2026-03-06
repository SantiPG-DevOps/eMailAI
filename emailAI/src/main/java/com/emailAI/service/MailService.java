package com.emailAI.service;

import com.emailAI.model.Mensaje;
import com.emailAI.ia.GestorModelos;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailService {

    private Session session;
    private Store store;
    private String currentUser;
    private String currentPassword;
    private String smtpHost = null;

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
    }

    /**
     * Descarga los últimos mensajes y los clasifica mediante IA (Spam y Prioridad).
     */
    public List<Mensaje> listInbox() throws Exception {
        List<Mensaje> resultado = new ArrayList<>();

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.getMessages();

        int total = messages.length;
        int max = 20; // último bloque de correos para procesar
        int inicio = Math.max(1, total - max + 1);

        for (int i = total; i >= inicio; i--) { // Iteramos de más reciente a más antiguo
            Message msg = messages[i - 1];
            String id = String.valueOf(msg.getMessageNumber());

            Address[] froms = msg.getFrom();
            String remitente = (froms != null && froms.length > 0)
                    ? ((InternetAddress) froms[0]).getAddress()
                    : "desconocido";

            String asunto = msg.getSubject();
            String cuerpo = extraerCuerpoTexto(msg);

            // Creamos el objeto mensaje
            Mensaje mensajeObj = new Mensaje(id, remitente, asunto, cuerpo);

            // --- INTEGRACIÓN DE INTELIGENCIA ARTIFICIAL ---
            try {
                // 1. Clasificación de SPAM
                String categoria = GestorModelos.clasificarSpam(mensajeObj);
                mensajeObj.setCategoria(categoria);

                // 2. Clasificación de PRIORIDAD (solo si es legítimo)
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
     * Extrae el contenido en texto plano del mensaje, manejando estructuras Multipart.
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
                    // Manejo recursivo simple para estructuras complejas
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
}