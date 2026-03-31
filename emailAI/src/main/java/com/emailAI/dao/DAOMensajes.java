package com.emailAI.dao;

import com.emailAI.model.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Gestiona la tabla de mensajes en SQLite: creación, lectura, upsert y operaciones auxiliares.
public class DAOMensajes {

    private final String url;

    // Recibe la URL de conexión (SQLite) y asegura que la tabla exista.
    public DAOMensajes(String url) {
        this.url = url;
        inicializarTabla();
    }

    // Crea la tabla de mensajes si no existe todavía.
    private void inicializarTabla() {
        String sql = """
                CREATE TABLE IF NOT EXISTS mensajes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uid_imap      TEXT NOT NULL,
                    cuenta_hash   TEXT NOT NULL,
                    remitente     TEXT,
                    asunto        TEXT,
                    cuerpo        TEXT,
                    fecha         TEXT,
                    categoria     TEXT,
                    prioridad     TEXT,
                    html          TEXT,
                    UNIQUE(uid_imap, cuenta_hash)
                );
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== CARGAR DESDE BD ==================

    // Devuelve todos los mensajes asociados a una cuenta ordenados por inserción descendente.
    public List<Mensaje> listarPorCuentaHash(String cuentaHash) {
        List<Mensaje> lista = new ArrayList<>();
        String sql = """
                SELECT uid_imap, remitente, asunto, cuerpo, fecha, categoria, prioridad, html
                FROM mensajes
                WHERE cuenta_hash = ?
                ORDER BY id DESC
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cuentaHash);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mensaje m = new Mensaje();
                    m.setUidImap(rs.getString("uid_imap"));
                    m.setRemitente(rs.getString("remitente"));
                    m.setAsunto(rs.getString("asunto"));
                    m.setCuerpo(rs.getString("cuerpo"));
                    m.setFecha(rs.getString("fecha"));
                    m.setCategoria(rs.getString("categoria"));
                    m.setPrioridad(rs.getString("prioridad"));
                    m.setHtml(rs.getString("html"));
                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ================== GUARDAR / MODIFICAR (UPSERT) ==================

    // Inserta o actualiza en bloque los mensajes recibidos para una cuenta (upsert por uid_imap + cuenta).
    public void guardarOModificar(List<Mensaje> mensajes, String cuentaHash) {
        if (mensajes == null || mensajes.isEmpty() || cuentaHash == null) return;

        String sql = """
                INSERT INTO mensajes
                    (uid_imap, cuenta_hash, remitente, asunto, cuerpo, fecha, categoria, prioridad, html)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uid_imap, cuenta_hash) DO UPDATE SET
                    remitente = excluded.remitente,
                    asunto    = excluded.asunto,
                    cuerpo    = excluded.cuerpo,
                    fecha     = excluded.fecha,
                    categoria = excluded.categoria,
                    prioridad = excluded.prioridad,
                    html      = excluded.html;
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (Mensaje m : mensajes) {
                if (m.getUidImap() == null) continue;

                ps.setString(1, m.getUidImap());
                ps.setString(2, cuentaHash);
                ps.setString(3, m.getRemitente());
                ps.setString(4, m.getAsunto());
                ps.setString(5, m.getCuerpo());
                ps.setString(6, m.getFecha());
                ps.setString(7, m.getCategoria());
                ps.setString(8, m.getPrioridad());
                ps.setString(9, m.getHtml());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== OPCIONAL: BORRAR POR CUENTA ==================

    // Borra todos los mensajes asociados a una cuenta dada.
    public void borrarPorCuenta(String cuentaHash) {
        String sql = "DELETE FROM mensajes WHERE cuenta_hash = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cuentaHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Actualiza solo la categoría y prioridad de un mensaje identificado por uid_imap y cuenta.
    public void actualizarCategoriaPrioridad(String uidImap, String cuentaHash, String categoria, String prioridad) {
        if (uidImap == null || cuentaHash == null) return;

        String sql = """
                UPDATE mensajes
                SET categoria = ?, prioridad = ?
                WHERE uid_imap = ? AND cuenta_hash = ?
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria);
            ps.setString(2, prioridad);
            ps.setString(3, uidImap);
            ps.setString(4, cuentaHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}