package com.emailAI.dao;

import com.emailAI.model.Tarea;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// DAO de tareas → agenda.db
public class DAOTareas {

    public DAOTareas() {
        inicializarTabla();
    }

    /** @deprecated Usar constructor sin argumentos. */
    @Deprecated
    public DAOTareas(String url) {
        inicializarTabla();
    }

    private void inicializarTabla() {
        String sql = """
                CREATE TABLE IF NOT EXISTS tareas (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo            TEXT NOT NULL,
                    descripcion       TEXT,
                    fecha_vencimiento TEXT,
                    estado            TEXT,
                    etiquetas         TEXT,
                    prioridad         TEXT DEFAULT 'MEDIA'
                )
                """;
        try (Connection conn = ConexionBD.getConnectionAgenda();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Tarea> listarTodas() {
        List<Tarea> lista = new ArrayList<>();
        String sql = """
                SELECT id, titulo, descripcion, fecha_vencimiento, estado, etiquetas, prioridad
                FROM tareas
                ORDER BY fecha_vencimiento IS NULL, fecha_vencimiento
                """;
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fechaStr = rs.getString("fecha_vencimiento");
                LocalDate fecha = (fechaStr != null && !fechaStr.isBlank())
                        ? LocalDate.parse(fechaStr) : null;
                lista.add(new Tarea(
                        rs.getInt("id"),
                        rs.getString("titulo"),
                        rs.getString("descripcion"),
                        fecha,
                        rs.getString("estado"),
                        rs.getString("etiquetas"),
                        rs.getString("prioridad")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public void guardarOActualizar(Tarea t) {
        if (t.getId() == null) insertar(t);
        else actualizar(t);
    }

    private void insertar(Tarea t) {
        String sql = """
                INSERT INTO tareas (titulo, descripcion, fecha_vencimiento, estado, etiquetas, prioridad)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitulo());
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : null);
            ps.setString(4, t.getEstado());
            ps.setString(5, t.getEtiquetas());
            ps.setString(6, t.getPrioridad() != null ? t.getPrioridad() : "MEDIA");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void actualizar(Tarea t) {
        String sql = """
                UPDATE tareas
                SET titulo = ?, descripcion = ?, fecha_vencimiento = ?, estado = ?, etiquetas = ?, prioridad = ?
                WHERE id = ?
                """;
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTitulo());
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : null);
            ps.setString(4, t.getEstado());
            ps.setString(5, t.getEtiquetas());
            ps.setString(6, t.getPrioridad() != null ? t.getPrioridad() : "MEDIA");
            ps.setInt(7, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void borrar(Tarea t) {
        if (t.getId() == null) return;
        String sql = "DELETE FROM tareas WHERE id = ?";
        try (Connection conn = ConexionBD.getConnectionAgenda();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}