package com.emailAI.model;

import java.time.LocalDate;

public class Tarea {

    private Integer id;
    private String titulo;
    private String descripcion;
    private LocalDate fechaVencimiento;   // due date
    private String estado;                // PENDIENTE, EN_PROGRESO, COMPLETADA
    private String etiquetas;             // tags separados por comas, de momento texto plano

    public Tarea() {}

    public Tarea(Integer id, String titulo, String descripcion,
                 LocalDate fechaVencimiento, String estado, String etiquetas) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.etiquetas = etiquetas;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEtiquetas() { return etiquetas; }
    public void setEtiquetas(String etiquetas) { this.etiquetas = etiquetas; }
}