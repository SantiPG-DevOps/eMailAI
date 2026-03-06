package com.emailAI.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DAOEventosCalendario {

    public record Evento(int id, LocalDate fecha, String titulo, String detalle, String origen) {}

    public DAOEventosCalendario() throws Exception {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws Exception {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS eventos_calendario (
                        id      INTEGER PRIMARY KEY AUTOINCREMENT,
                        fecha   TEXT NOT NULL,
                        titulo  TEXT NOT NULL,
                        detalle TEXT,
                        origen  TEXT DEFAULT 'local'
                    )
                """)) {
            st.executeUpdate();
        }
    }

    public void guardarEvento(LocalDate fecha, String titulo, String detalle, String origen) throws Exception {
        String sql = "INSERT INTO eventos_calendario(fecha, titulo, detalle, origen) VALUES(?, ?, ?, ?)";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, fecha.toString());
            st.setString(2, titulo);
            st.setString(3, detalle);
            st.setString(4, origen != null ? origen : "local");
            st.executeUpdate();
        }
    }

    public List<Evento> listarPorFecha(LocalDate fecha) throws Exception {
        String sql = "SELECT id, fecha, titulo, detalle, origen FROM eventos_calendario WHERE fecha = ? ORDER BY id";
        List<Evento> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, fecha.toString());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Evento(
                            rs.getInt("id"),
                            LocalDate.parse(rs.getString("fecha")),
                            rs.getString("titulo"),
                            rs.getString("detalle"),
                            rs.getString("origen")
                    ));
                }
            }
        }
        return lista;
    }

    public void borrarEvento(int id) throws Exception {
        String sql = "DELETE FROM eventos_calendario WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }
}
