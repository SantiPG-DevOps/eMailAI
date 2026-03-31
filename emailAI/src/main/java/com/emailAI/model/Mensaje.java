package com.emailAI.model;

// Modelo de dominio que representa un correo con metadatos, etiquetas de IA y campos auxiliares.
public class Mensaje {

    // Almacena el identificador lógico del mensaje en el sistema.
    private String id;

    private String remitente;
    private String asunto;
    private String cuerpo;      // texto plano (para BD + IA)
    private String fecha;
    private String uidImap;     // UID real IMAP
    // Guarda la categoría inferida o asignada para clasificación del correo.
    private String categoria;
    private String prioridad;   // URGENTE / NORMAL / etc.
    private String cuentaHash;  // hash de la cuenta
    private String html;        // cuerpo HTML solo para interfaz

    // Guarda campos adicionales generados por servicios IA.
    private String resumenIA;
    private String sugerenciaIA;

    // Constructor vacío usado por frameworks y DAOs.
    public Mensaje() {
    }

    // Constructor rápido para instanciar un mensaje mínimo desde IMAP.
    public Mensaje(String id, String remitente, String asunto, String cuerpo) {
        this.id = id;
        this.remitente = remitente;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getUidImap() {
        return uidImap;
    }

    public void setUidImap(String uidImap) {
        this.uidImap = uidImap;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getCuentaHash() {
        return cuentaHash;
    }

    public void setCuentaHash(String cuentaHash) {
        this.cuentaHash = cuentaHash;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getResumenIA() {
        return resumenIA;
    }

    public void setResumenIA(String resumenIA) {
        this.resumenIA = resumenIA;
    }

    public String getSugerenciaIA() {
        return sugerenciaIA;
    }

    public void setSugerenciaIA(String sugerenciaIA) {
        this.sugerenciaIA = sugerenciaIA;
    }

    @Override
    public String toString() {
        String r = remitente != null ? remitente : "";
        String a = asunto != null ? asunto : "";
        return r + " - " + a;
    }
}