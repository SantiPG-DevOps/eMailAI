package com.emailAI.dao;

import com.emailAI.model.Tarea;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// DAO de tareas: creación de tabla y operaciones CRUD sobre la lista de pendientes.
public class DAOTareas {

    private final String url; // URL de conexión SQLite usada por este DAO.

    // Inicializa DAO con su conexión y crea la tabla si falta.
    public DAOTareas(String url) {
        this.url = url;
        inicializarTabla();
    }

    // Crea la tabla de tareas en la base local.
    private void inicializarTabla() {
        String sql = """
                CREATE TABLE IF NOT EXISTS tareas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT NOT NULL,
                    descripcion TEXT,
                    fecha_vencimiento TEXT,
                    estado TEXT,
                    etiquetas TEXT
                )
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lista todas las tareas ordenadas por fecha de vencimiento.
    public List<Tarea> listarTodas() {
        List<Tarea> lista = new ArrayList<>();
        String sql = "SELECT id, titulo, descripcion, fecha_vencimiento, estado, etiquetas FROM tareas ORDER BY fecha_vencimiento IS NULL, fecha_vencimiento";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Integer id = rs.getInt("id");
                String titulo = rs.getString("titulo");
                String desc = rs.getString("descripcion");
                String fechaStr = rs.getString("fecha_vencimiento");
                String estado = rs.getString("estado");
                String etiquetas = rs.getString("etiquetas");

                LocalDate fecha = null;
                if (fechaStr != null && !fechaStr.isBlank()) {
                    fecha = LocalDate.parse(fechaStr);
                }

                lista.add(new Tarea(id, titulo, desc, fecha, estado, etiquetas));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    // Decide insertar o actualizar según si la tarea ya tiene id.
    public void guardarOActualizar(Tarea t) {
        if (t.getId() == null) {
            insertar(t);
        } else {
            actualizar(t);
        }
    }

    // Inserta una tarea nueva y sincroniza el id generado en el objeto.
    private void insertar(Tarea t) {
        String sql = """
                INSERT INTO tareas (titulo, descripcion, fecha_vencimiento, estado, etiquetas)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getTitulo());
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : null);
            ps.setString(4, t.getEstado());
            ps.setString(5, t.getEtiquetas());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Actualiza una tarea existente usando su id como clave.
    private void actualizar(Tarea t) {
        String sql = """
                UPDATE tareas
                SET titulo = ?, descripcion = ?, fecha_vencimiento = ?, estado = ?, etiquetas = ?
                WHERE id = ?
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, t.getTitulo());
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : null);
            ps.setString(4, t.getEstado());
            ps.setString(5, t.getEtiquetas());
            ps.setInt(6, t.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Elimina una tarea por id si existe en BD.
    public void borrar(Tarea t) {
        if (t.getId() == null) return;

        String sql = "DELETE FROM tareas WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}