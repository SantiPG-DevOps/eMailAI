package com.emailAI.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// DAO de eventos de calendario → agenda.db (compartida con tareas)
public class DAOEventosCalendario {

    public record Evento(int id, LocalDate fecha, LocalTime hora,
                         String titulo, String detalle, String origen) {}

    public DAOEventosCalendario() throws Exception {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS eventos_calendario (
                    id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha   TEXT NOT NULL,
                    hora    TEXT,
                    titulo  TEXT NOT NULL,
                    detalle TEXT,
                    origen  TEXT DEFAULT 'local'
                )
                """;
        try (Connection conn = ConexionBD.getConnectionAgenda();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public void guardarEvento(LocalDate fecha, LocalTime hora,
                               String titulo, String detalle, String origen) throws Exception {
        String sql = "INSERT INTO eventos_calendario(fecha, hora, titulo, detalle, origen) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, fecha.toString());
            st.setString(2, hora != null ? hora.toString() : null);
            st.setString(3, titulo);
            st.setString(4, detalle);
            st.setString(5, origen != null ? origen : "local");
            st.executeUpdate();
        }
    }

    public List<Evento> listarPorFecha(LocalDate fecha) throws Exception {
        String sql = """
                SELECT id, fecha, hora, titulo, detalle, origen
                FROM eventos_calendario
                WHERE fecha = ?
                ORDER BY hora, id
                """;
        List<Evento> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, fecha.toString());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String horaStr = rs.getString("hora");
                    lista.add(new Evento(
                            rs.getInt("id"),
                            LocalDate.parse(rs.getString("fecha")),
                            horaStr != null ? LocalTime.parse(horaStr) : null,
                            rs.getString("titulo"),
                            rs.getString("detalle"),
                            rs.getString("origen")
                    ));
                }
            }
        }
        return lista;
    }

    public Set<LocalDate> fechasConEventosEnRango(LocalDate desde, LocalDate hasta) throws Exception {
        if (desde == null || hasta == null) return Set.of();
        String sql = """
                SELECT DISTINCT fecha FROM eventos_calendario
                WHERE fecha >= ? AND fecha <= ?
                """;
        Set<LocalDate> set = new HashSet<>();
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, desde.toString());
            st.setString(2, hasta.toString());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) set.add(LocalDate.parse(rs.getString("fecha")));
            }
        }
        return set;
    }

    public void borrarEvento(int id) throws Exception {
        String sql = "DELETE FROM eventos_calendario WHERE id = ?";
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }

    public void actualizarEvento(Evento evento) throws Exception {
        String sql = """
                UPDATE eventos_calendario
                SET fecha = ?, hora = ?, titulo = ?, detalle = ?, origen = ?
                WHERE id = ?
                """;
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, evento.fecha().toString());
            st.setString(2, evento.hora() != null ? evento.hora().toString() : null);
            st.setString(3, evento.titulo());
            st.setString(4, evento.detalle());
            st.setString(5, evento.origen());
            st.setInt(6, evento.id());
            st.executeUpdate();
        }
    }
    
    public List<Evento> listarEventos() throws Exception {
        String sql = """
                SELECT id, fecha, hora, titulo, detalle, origen
                FROM eventos_calendario
                ORDER BY fecha, hora
                """;
        List<Evento> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(new Evento(
                        rs.getInt("id"),
                        LocalDate.parse(rs.getString("fecha")),
                        rs.getString("hora") != null ? LocalTime.parse(rs.getString("hora")) : null,
                        rs.getString("titulo"),
                        rs.getString("detalle"),
                        rs.getString("origen")
                ));
            }
        }
        return lista;
    }
}