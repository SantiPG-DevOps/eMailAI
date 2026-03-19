package com.emailAI.dao;

import com.emailAI.security.UtilidadCifrado;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DAOTareas {

    public enum Estado {
        PENDIENTE,
        COMPLETADA
    }

    public static class Tarea {
        private final int id;
        private final int cuentaId;
        private final String titulo;
        private final String descripcion;
        private final LocalDate fechaLimite;
        private final Estado estado;

        public Tarea(int id, int cuentaId, String titulo, String descripcion,
                     LocalDate fechaLimite, Estado estado) {
            this.id = id;
            this.cuentaId = cuentaId;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.fechaLimite = fechaLimite;
            this.estado = estado;
        }

        public int getId() { return id; }
        public int getCuentaId() { return cuentaId; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public LocalDate getFechaLimite() { return fechaLimite; }
        public Estado getEstado() { return estado; }

        public boolean estaCompletada() {
            return estado == Estado.COMPLETADA;
        }

        @Override
        public String toString() {
            return titulo;
        }
    }

    private final String claveCifrado;

    public DAOTareas(String claveCifrado) throws Exception {
        this.claveCifrado = claveCifrado;
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS tareas (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    cuenta_id    INTEGER NOT NULL,
                    titulo       TEXT NOT NULL,
                    descripcion  TEXT,
                    fecha_limite TEXT,
                    estado       TEXT NOT NULL DEFAULT 'PENDIENTE',
                    FOREIGN KEY (cuenta_id) REFERENCES cuentas(id)
                )
                """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.executeUpdate();
        }
    }

    public void insertarTarea(int cuentaId, String titulo, String descripcion,
                              LocalDate fechaLimite) throws Exception {
        String sql = "INSERT INTO tareas(cuenta_id, titulo, descripcion, fecha_limite, estado) VALUES(?,?,?,?,?)";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, cuentaId);
            st.setString(2, UtilidadCifrado.cifrar(titulo, claveCifrado));
            st.setString(3, descripcion != null && !descripcion.isEmpty()
                    ? UtilidadCifrado.cifrar(descripcion, claveCifrado) : null);
            st.setString(4, fechaLimite != null ? fechaLimite.toString() : null);
            st.setString(5, Estado.PENDIENTE.name());

            st.executeUpdate();
        }
    }

    public List<Tarea> listarPorCuenta(int cuentaId) throws Exception {
        String sql = "SELECT id, cuenta_id, titulo, descripcion, fecha_limite, estado FROM tareas WHERE cuenta_id = ? ORDER BY id DESC";
        List<Tarea> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, cuentaId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int cid = rs.getInt("cuenta_id");

                    String titulo = UtilidadCifrado.descifrar(rs.getString("titulo"), claveCifrado);

                    String descCif = rs.getString("descripcion");
                    String descripcion = descCif != null ? UtilidadCifrado.descifrar(descCif, claveCifrado) : "";

                    String fechaTxt = rs.getString("fecha_limite");
                    LocalDate fechaLimite = fechaTxt != null ? LocalDate.parse(fechaTxt) : null;

                    Estado estado = Estado.valueOf(rs.getString("estado"));

                    lista.add(new Tarea(id, cid, titulo, descripcion, fechaLimite, estado));
                }
            }
        }
        return lista;
    }

    public void actualizarEstado(int tareaId, Estado nuevoEstado) throws Exception {
        String sql = "UPDATE tareas SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, nuevoEstado.name());
            st.setInt(2, tareaId);
            st.executeUpdate();
        }
    }

    public void borrarTarea(int tareaId) throws Exception {
        String sql = "DELETE FROM tareas WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, tareaId);
            st.executeUpdate();
        }
    }
}
