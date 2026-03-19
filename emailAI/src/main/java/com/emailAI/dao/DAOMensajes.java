package com.emailAI.dao;

import com.emailAI.model.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOMensajes {

    public DAOMensajes() throws SQLException {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws SQLException {
        String createSql = """
                CREATE TABLE IF NOT EXISTS mensajes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uid_imap TEXT NOT NULL UNIQUE,
                    cuenta_hash TEXT NOT NULL,
                    remitente TEXT NOT NULL,
                    asunto TEXT NOT NULL,
                    cuerpo TEXT,
                    html TEXT,
                    fecha TEXT,
                    categoria TEXT,
                    prioridad TEXT
                );
                """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(createSql)) {
            ps.execute();
        }

        // Si la tabla ya existía sin la columna html, la añadimos
        try (Connection conn = ConexionBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(mensajes)")) {

            boolean tieneHtml = false;
            while (rs.next()) {
                String colName = rs.getString("name");
                if ("html".equalsIgnoreCase(colName)) {
                    tieneHtml = true;
                    break;
                }
            }

            if (!tieneHtml) {
                try (Statement alter = conn.createStatement()) {
                    alter.execute("ALTER TABLE mensajes ADD COLUMN html TEXT");
                }
            }
        }
    }

    /**
     * Lista todos los mensajes de una cuenta (cuentaHash) ordenados por id descendente (más recientes primero).
     */
    public List<Mensaje> listarPorCuenta(String cuentaHash) throws SQLException {
        List<Mensaje> lista = new ArrayList<>();

        String sql = """
                SELECT uid_imap, remitente, asunto, cuerpo, html, fecha, categoria, prioridad
                  FROM mensajes
                 WHERE cuenta_hash = ?
                 ORDER BY id DESC
                """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cuentaHash);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mensaje m = new Mensaje();
                    m.setUidImap(rs.getString("uid_imap"));
                    m.setRemitente(rs.getString("remitente"));
                    m.setAsunto(rs.getString("asunto"));
                    m.setCuerpo(rs.getString("cuerpo"));
                    m.setHtml(rs.getString("html"));
                    m.setFecha(rs.getString("fecha"));
                    m.setCategoria(rs.getString("categoria"));
                    m.setPrioridad(rs.getString("prioridad"));
                    m.setCuentaHash(cuentaHash);
                    lista.add(m);
                }
            }
        }

        return lista;
    }

    /**
     * Guarda o actualiza una lista de mensajes para una cuenta concreta.
     * Si (uid_imap, cuenta_hash) existe -> UPDATE.
     * Si no existe -> INSERT.
     */
    public void guardarOModificar(String cuentaHash, List<Mensaje> lista) throws SQLException {
        String sqlExiste = "SELECT id FROM mensajes WHERE uid_imap = ? AND cuenta_hash = ?";

        String sqlInsert = """
                INSERT INTO mensajes (uid_imap, cuenta_hash, remitente, asunto, cuerpo, html, fecha, categoria, prioridad)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String sqlUpdate = """
                UPDATE mensajes
                   SET remitente = ?, asunto = ?, cuerpo = ?, html = ?, fecha = ?, categoria = ?, prioridad = ?
                 WHERE uid_imap = ? AND cuenta_hash = ?
                """;

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psExiste = conn.prepareStatement(sqlExiste);
                 PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
                 PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {

                for (Mensaje m : lista) {

                    // Seguridad: uid_imap no puede ser null porque la columna es NOT NULL
                    if (m.getUidImap() == null || m.getUidImap().isBlank()) {
                        // si llega null, lo saltamos para no romper la transacción
                        continue;
                    }

                    // ¿existe ya este mensaje para esa cuenta?
                    psExiste.setString(1, m.getUidImap());
                    psExiste.setString(2, cuentaHash);

                    try (ResultSet rs = psExiste.executeQuery()) {
                        if (rs.next()) {
                            // UPDATE
                            psUpdate.setString(1, m.getRemitente());
                            psUpdate.setString(2, m.getAsunto());
                            psUpdate.setString(3, m.getCuerpo());
                            psUpdate.setString(4, m.getHtml());
                            psUpdate.setString(5, m.getFecha());
                            psUpdate.setString(6, m.getCategoria());
                            psUpdate.setString(7, m.getPrioridad());
                            psUpdate.setString(8, m.getUidImap());
                            psUpdate.setString(9, cuentaHash);
                            psUpdate.addBatch();
                        } else {
                            // INSERT
                            psInsert.setString(1, m.getUidImap());
                            psInsert.setString(2, cuentaHash);
                            psInsert.setString(3, m.getRemitente());
                            psInsert.setString(4, m.getAsunto());
                            psInsert.setString(5, m.getCuerpo());
                            psInsert.setString(6, m.getHtml());
                            psInsert.setString(7, m.getFecha());
                            psInsert.setString(8, m.getCategoria());
                            psInsert.setString(9, m.getPrioridad());
                            psInsert.addBatch();
                        }
                    }
                }

                psUpdate.executeBatch();
                psInsert.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
