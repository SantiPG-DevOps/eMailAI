package com.emailAI.model;

public class Mensaje {

    private String id;         // puede ser el messageNumber/UID
    private String remitente;
    private String asunto;
    private String cuerpo;
    private String fecha;
    private String uidImap;
    private String categoria;  // SPAM / LEGITIMO / DESCONOCIDO
    private String prioridad;  // URGENTE / NORMAL
    private String cuentaHash; // para vincular a la cuenta en BD

    // Constructor vacío NECESARIO para DAOMensajes
    public Mensaje() {
    }

    // Tu constructor actual (lo mantenemos)
    public Mensaje(String id, String remitente, String asunto, String cuerpo) {
        this.id = id;
        this.remitente = remitente;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }

    // ========== GETTERS Y SETTERS ==========

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
}
