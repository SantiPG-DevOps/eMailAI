package com.emailAI.model;

public class Mensaje {
    private String id;
    private String remitente;
    private String asunto;
    private String cuerpo;
    
    // Atributos para almacenar los resultados de la IA
    private String categoria = "LEGITIMO"; // SPAM o LEGITIMO
    private String prioridad = "NORMAL";   // URGENTE o NORMAL

    public Mensaje(String id, String remitente, String asunto, String cuerpo) {
        this.id = id;
        this.remitente = remitente;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }

    // Getters y Setters necesarios para el MailService y la UI
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getId() { return id; }
    public String getRemitente() { return remitente; }
    public String getAsunto() { return asunto; }
    public String getCuerpo() { return cuerpo; }
}