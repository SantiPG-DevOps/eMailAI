package com.emailAI.model;

public class Mensaje {

    private Long id;

    private String uidImap;
    private String cuentaHash;
    private String carpetaImap;

    private String remitente;
    private String destinatarios;
    private String cc;
    private String cco;
    private String asunto;
    private String cuerpo;
    private String html;

    private String fecha;

    private String categoria;
    private String prioridad;

    private String resumenIA;
    private String sugerenciaIA;

    public Mensaje() {
    }

    public Mensaje(String uidImap, String remitente, String asunto, String cuerpo) {
        this.uidImap = uidImap;
        this.remitente = remitente;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.categoria = "DESCONOCIDO";
        this.prioridad = "NORMAL";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUidImap() {
        return uidImap;
    }

    public void setUidImap(String uidImap) {
        this.uidImap = uidImap;
    }

    public String getCuentaHash() {
        return cuentaHash;
    }

    public void setCuentaHash(String cuentaHash) {
        this.cuentaHash = cuentaHash;
    }

    public String getCarpetaImap() {
        return carpetaImap;
    }

    public void setCarpetaImap(String carpetaImap) {
        this.carpetaImap = carpetaImap;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(String destinatarios) {
        this.destinatarios = destinatarios;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getCco() {
        return cco;
    }

    public void setCco(String cco) {
        this.cco = cco;
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

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
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

    public String getFechaRecepcion() {
        return fecha;
    }

    public void setFechaRecepcion(String fechaRecepcion) {
        this.fecha = fechaRecepcion;
    }

    @Override
    public String toString() {
        String rem = remitente != null ? remitente : "";
        String asu = asunto != null ? asunto : "";
        return rem + " - " + asu;
    }
}