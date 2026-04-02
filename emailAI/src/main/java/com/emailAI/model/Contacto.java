package com.emailAI.model;

public class Contacto {

    private Integer id;
    private String nombre;
    private String apellido;   // ← nuevo
    private String email;
    private String telefono;
    private String notas;

    public Contacto() {}

    // Constructor legacy (sin apellido) — mantiene compatibilidad
    public Contacto(Integer id, String nombre, String email, String telefono, String notas) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = null;
        this.email = email;
        this.telefono = telefono;
        this.notas = notas;
    }

    // Constructor completo
    public Contacto(Integer id, String nombre, String apellido, String email, String telefono, String notas) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.notas = notas;
    }

    // Devuelve "Apellido, Nombre" o solo nombre si no hay apellido — usado para ordenar y mostrar
    public String getNombreCompleto() {
        if (apellido != null && !apellido.isBlank()) {
            return apellido.trim() + ", " + (nombre != null ? nombre.trim() : "");
        }
        return nombre != null ? nombre.trim() : "";
    }

    public Integer getId()                  { return id; }
    public void    setId(Integer id)        { this.id = id; }

    public String  getNombre()              { return nombre; }
    public void    setNombre(String n)      { this.nombre = n; }

    public String  getApellido()            { return apellido; }
    public void    setApellido(String a)    { this.apellido = a; }

    public String  getEmail()               { return email; }
    public void    setEmail(String e)       { this.email = e; }

    public String  getTelefono()            { return telefono; }
    public void    setTelefono(String t)    { this.telefono = t; }

    public String  getNotas()               { return notas; }
    public void    setNotas(String n)       { this.notas = n; }
}