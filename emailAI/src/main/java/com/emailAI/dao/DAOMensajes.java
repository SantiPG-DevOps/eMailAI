package com.emailAI.dao;

import com.emailAI.model.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Gestiona la tabla de mensajes en SQLite: creación, lectura, upsert y operaciones auxiliares.
public class DAOMensajes {

    private final String url;

    public DAOMensajes(String url) {
        this.url = url;
        inicializarTabla();
    }

    private void inicializarTabla() {
        String sqlNueva = """
                CREATE TABLE IF NOT EXISTS mensajes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uid_imap      TEXT NOT NULL,
                    cuenta_hash   TEXT NOT NULL,
                    carpeta_imap  TEXT NOT NULL DEFAULT 'INBOX',
                    remitente     TEXT,
                    asunto        TEXT,
                    cuerpo        TEXT,
                    fecha         TEXT,
                    categoria     TEXT,
                    prioridad     TEXT,
                    html          TEXT,
                    UNIQUE(uid_imap, cuenta_hash, carpeta_imap)
                );
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute(sqlNueva);
            if (!tablaTieneColumna(conn, "mensajes", "carpeta_imap")) {
                migrarAnadirCarpetaImap(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean tablaTieneColumna(Connection conn, String tabla, String columna) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tabla, columna)) {
            return rs.next();
        }
    }

    /**
     * Migra esquema antiguo UNIQUE(uid_imap, cuenta_hash) al nuevo con carpeta_imap.
     */
    private void migrarAnadirCarpetaImap(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE mensajes RENAME TO mensajes_legacy");
            st.execute("""
                    CREATE TABLE mensajes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uid_imap      TEXT NOT NULL,
                        cuenta_hash   TEXT NOT NULL,
                        carpeta_imap  TEXT NOT NULL DEFAULT 'INBOX',
                        remitente     TEXT,
                        asunto        TEXT,
                        cuerpo        TEXT,
                        fecha         TEXT,
                        categoria     TEXT,
                        prioridad     TEXT,
                        html          TEXT,
                        UNIQUE(uid_imap, cuenta_hash, carpeta_imap)
                    );
                    """);
            st.executeUpdate("""
                    INSERT INTO mensajes
                    (id, uid_imap, cuenta_hash, carpeta_imap, remitente, asunto, cuerpo, fecha, categoria, prioridad, html)
                    SELECT id, uid_imap, cuenta_hash, 'INBOX', remitente, asunto, cuerpo, fecha, categoria, prioridad, html
                    FROM mensajes_legacy
                    """);
            st.executeUpdate("DROP TABLE mensajes_legacy");
        }
    }

    public List<Mensaje> listarPorCuentaHashYCarpeta(String cuentaHash, String carpetaImap) {
        List<Mensaje> lista = new ArrayList<>();
        if (cuentaHash == null || carpetaImap == null) {
            return lista;
        }
        String sql = """
                SELECT uid_imap, carpeta_imap, remitente, asunto, cuerpo, fecha, categoria, prioridad, html
                FROM mensajes
                WHERE cuenta_hash = ? AND carpeta_imap = ?
                ORDER BY id DESC
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cuentaHash);
            ps.setString(2, carpetaImap);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerMensajeDesdeRs(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /** Todos los mensajes de la cuenta (p. ej. entrenamiento IA). */
    public List<Mensaje> listarTodosPorCuenta(String cuentaHash) {
        List<Mensaje> lista = new ArrayList<>();
        if (cuentaHash == null) {
            return lista;
        }
        String sql = """
                SELECT uid_imap, carpeta_imap, remitente, asunto, cuerpo, fecha, categoria, prioridad, html
                FROM mensajes
                WHERE cuenta_hash = ?
                ORDER BY id DESC
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cuentaHash);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerMensajeDesdeRs(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private static Mensaje leerMensajeDesdeRs(ResultSet rs) throws SQLException {
        Mensaje m = new Mensaje();
        m.setUidImap(rs.getString("uid_imap"));
        m.setCarpetaImap(rs.getString("carpeta_imap"));
        m.setRemitente(rs.getString("remitente"));
        m.setAsunto(rs.getString("asunto"));
        m.setCuerpo(rs.getString("cuerpo"));
        m.setFecha(rs.getString("fecha"));
        m.setCategoria(rs.getString("categoria"));
        m.setPrioridad(rs.getString("prioridad"));
        m.setHtml(rs.getString("html"));
        return m;
    }

    public void guardarOModificar(List<Mensaje> mensajes, String cuentaHash, String carpetaImap) {
        if (mensajes == null || mensajes.isEmpty() || cuentaHash == null || carpetaImap == null) {
            return;
        }

        String sql = """
                INSERT INTO mensajes
                    (uid_imap, cuenta_hash, carpeta_imap, remitente, asunto, cuerpo, fecha, categoria, prioridad, html)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uid_imap, cuenta_hash, carpeta_imap) DO UPDATE SET
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
                ps.setString(3, carpetaImap);
                ps.setString(4, m.getRemitente());
                ps.setString(5, m.getAsunto());
                ps.setString(6, m.getCuerpo());
                ps.setString(7, m.getFecha());
                ps.setString(8, m.getCategoria());
                ps.setString(9, m.getPrioridad());
                ps.setString(10, m.getHtml());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

    public void actualizarCategoriaPrioridad(
            String uidImap,
            String cuentaHash,
            String carpetaImap,
            String categoria,
            String prioridad) {
        if (uidImap == null || cuentaHash == null || carpetaImap == null) {
            return;
        }

        String sql = """
                UPDATE mensajes
                SET categoria = ?, prioridad = ?
                WHERE uid_imap = ? AND cuenta_hash = ? AND carpeta_imap = ?
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria);
            ps.setString(2, prioridad);
            ps.setString(3, uidImap);
            ps.setString(4, cuentaHash);
            ps.setString(5, carpetaImap);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
