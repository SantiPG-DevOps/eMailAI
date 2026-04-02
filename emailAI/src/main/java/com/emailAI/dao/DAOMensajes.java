package com.emailAI.dao;

import com.emailAI.model.Mensaje;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DAOMensajes {

    private static final int MAX_SPAM_ACTIVOS = 100;
    private static final int MAX_NO_SPAM_ACTIVOS = 100;

    public DAOMensajes() throws SQLException {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS mensajes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid_imap TEXT NOT NULL,
                cuenta_hash TEXT NOT NULL,
                carpeta_imap TEXT NOT NULL,
                remitente TEXT,
                asunto TEXT,
                cuerpo TEXT,
                html TEXT,
                categoria TEXT,
                prioridad TEXT,
                fecha_recepcion TEXT NOT NULL,
                UNIQUE(uid_imap, cuenta_hash, carpeta_imap)
            )
            """;

        try (Connection conn = ConexionBD.getConnectionCorreos();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public void guardarOModificar(List<Mensaje> mensajes, String cuentaHash, String carpeta) throws SQLException {
        String sql = """
            INSERT INTO mensajes (
                uid_imap, cuenta_hash, carpeta_imap, remitente, asunto, cuerpo,
                html, categoria, prioridad, fecha_recepcion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uid_imap, cuenta_hash, carpeta_imap) DO UPDATE SET
                remitente = excluded.remitente,
                asunto = excluded.asunto,
                cuerpo = excluded.cuerpo,
                html = excluded.html,
                categoria = excluded.categoria,
                prioridad = excluded.prioridad,
                fecha_recepcion = excluded.fecha_recepcion
            """;

        try (Connection conn = ConexionBD.getConnectionCorreos()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Mensaje m : mensajes) {
                    ps.setString(1, safe(m.getUidImap()));
                    ps.setString(2, cuentaHash);
                    ps.setString(3, carpeta);
                    ps.setString(4, safe(m.getRemitente()));
                    ps.setString(5, safe(m.getAsunto()));
                    ps.setString(6, safe(m.getCuerpo()));
                    ps.setString(7, safe(m.getHtml()));
                    ps.setString(8, safe(m.getCategoria()));
                    ps.setString(9, safe(m.getPrioridad()));
                    ps.setString(10, obtenerFechaRecepcion(m));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            limpiarSpamExcedente(conn, cuentaHash, carpeta);
            archivarNoSpamExcedente(conn, cuentaHash, carpeta);

            conn.commit();
        } catch (SQLException e) {
            throw e;
        }
    }

    public List<Mensaje> listarPorCuentaHashYCarpeta(String cuentaHash, String carpeta) throws SQLException {
        String sql = """
            SELECT uid_imap, remitente, asunto, cuerpo, html, categoria, prioridad, carpeta_imap, fecha_recepcion
            FROM mensajes
            WHERE cuenta_hash = ? AND carpeta_imap = ?
            ORDER BY fecha_recepcion DESC, id DESC
            """;

        List<Mensaje> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnectionCorreos();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cuentaHash);
            ps.setString(2, carpeta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(desdeResultSet(rs));
                }
            }
        }

        return lista;
    }

    public List<Mensaje> listarTodosPorCuenta(String cuentaHash) throws SQLException {
        String sql = """
            SELECT uid_imap, remitente, asunto, cuerpo, html, categoria, prioridad, carpeta_imap, fecha_recepcion
            FROM mensajes
            WHERE cuenta_hash = ?
            ORDER BY fecha_recepcion DESC, id DESC
            """;

        List<Mensaje> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnectionCorreos();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cuentaHash);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(desdeResultSet(rs));
                }
            }
        }

        return lista;
    }

    public void actualizarCategoriaPrioridad(String uidImap, String cuentaHash, String carpeta,
                                             String categoria, String prioridad) throws SQLException {
        String sql = """
            UPDATE mensajes
            SET categoria = ?, prioridad = ?
            WHERE uid_imap = ? AND cuenta_hash = ? AND carpeta_imap = ?
            """;

        try (Connection conn = ConexionBD.getConnectionCorreos();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria);
            ps.setString(2, prioridad);
            ps.setString(3, uidImap);
            ps.setString(4, cuentaHash);
            ps.setString(5, carpeta);
            ps.executeUpdate();
        }
    }

    private void limpiarSpamExcedente(Connection conn, String cuentaHash, String carpeta) throws SQLException {
        String sql = """
            DELETE FROM mensajes
            WHERE cuenta_hash = ?
              AND carpeta_imap = ?
              AND UPPER(COALESCE(categoria, '')) = 'SPAM'
              AND id NOT IN (
                  SELECT id
                  FROM mensajes
                  WHERE cuenta_hash = ?
                    AND carpeta_imap = ?
                    AND UPPER(COALESCE(categoria, '')) = 'SPAM'
                  ORDER BY fecha_recepcion DESC, id DESC
                  LIMIT ?
              )
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cuentaHash);
            ps.setString(2, carpeta);
            ps.setString(3, cuentaHash);
            ps.setString(4, carpeta);
            ps.setInt(5, MAX_SPAM_ACTIVOS);
            ps.executeUpdate();
        }
    }

    private void archivarNoSpamExcedente(Connection connPrincipal, String cuentaHash, String carpeta) throws SQLException {
        List<MensajeArchivado> excedentes = obtenerNoSpamExcedente(connPrincipal, cuentaHash, carpeta);

        Map<Integer, List<MensajeArchivado>> porAnio = excedentes.stream()
                .collect(Collectors.groupingBy(m -> extraerAnio(m.fechaRecepcion())));

        for (Map.Entry<Integer, List<MensajeArchivado>> entry : porAnio.entrySet()) {
            int anio = entry.getKey();
            List<MensajeArchivado> mensajes = entry.getValue();

            try (Connection connArchivo = ConexionBD.getConnectionArchivoCorreos(anio)) {
                connArchivo.setAutoCommit(false);

                crearTablaArchivoSiNoExiste(connArchivo);

                String insert = """
                    INSERT OR IGNORE INTO mensajes
                    (id, uid_imap, cuenta_hash, carpeta_imap, remitente, asunto, cuerpo, html, categoria, prioridad, fecha_recepcion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement ps = connArchivo.prepareStatement(insert)) {
                    for (MensajeArchivado m : mensajes) {
                        ps.setLong(1, m.id());
                        ps.setString(2, m.uidImap());
                        ps.setString(3, m.cuentaHash());
                        ps.setString(4, m.carpetaImap());
                        ps.setString(5, m.remitente());
                        ps.setString(6, m.asunto());
                        ps.setString(7, m.cuerpo());
                        ps.setString(8, m.html());
                        ps.setString(9, m.categoria());
                        ps.setString(10, m.prioridad());
                        ps.setString(11, m.fechaRecepcion());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                connArchivo.commit();
            }

            borrarArchivadosDePrincipal(connPrincipal, mensajes);
        }
    }

    private List<MensajeArchivado> obtenerNoSpamExcedente(Connection conn, String cuentaHash, String carpeta) throws SQLException {
        String sql = """
            SELECT id, uid_imap, cuenta_hash, carpeta_imap, remitente, asunto, cuerpo, html,
                   categoria, prioridad, fecha_recepcion
            FROM mensajes
            WHERE cuenta_hash = ?
              AND carpeta_imap = ?
              AND UPPER(COALESCE(categoria, '')) <> 'SPAM'
              AND id NOT IN (
                  SELECT id
                  FROM mensajes
                  WHERE cuenta_hash = ?
                    AND carpeta_imap = ?
                    AND UPPER(COALESCE(categoria, '')) <> 'SPAM'
                  ORDER BY fecha_recepcion DESC, id DESC
                  LIMIT ?
              )
            ORDER BY fecha_recepcion ASC, id ASC
            """;

        List<MensajeArchivado> lista = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cuentaHash);
            ps.setString(2, carpeta);
            ps.setString(3, cuentaHash);
            ps.setString(4, carpeta);
            ps.setInt(5, MAX_NO_SPAM_ACTIVOS);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MensajeArchivado(
                            rs.getLong("id"),
                            rs.getString("uid_imap"),
                            rs.getString("cuenta_hash"),
                            rs.getString("carpeta_imap"),
                            rs.getString("remitente"),
                            rs.getString("asunto"),
                            rs.getString("cuerpo"),
                            rs.getString("html"),
                            rs.getString("categoria"),
                            rs.getString("prioridad"),
                            rs.getString("fecha_recepcion")
                    ));
                }
            }
        }

        return lista;
    }

    private void borrarArchivadosDePrincipal(Connection conn, List<MensajeArchivado> mensajes) throws SQLException {
        String sql = "DELETE FROM mensajes WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (MensajeArchivado m : mensajes) {
                ps.setLong(1, m.id());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void crearTablaArchivoSiNoExiste(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS mensajes (
                id INTEGER PRIMARY KEY,
                uid_imap TEXT NOT NULL,
                cuenta_hash TEXT NOT NULL,
                carpeta_imap TEXT NOT NULL,
                remitente TEXT,
                asunto TEXT,
                cuerpo TEXT,
                html TEXT,
                categoria TEXT,
                prioridad TEXT,
                fecha_recepcion TEXT NOT NULL,
                UNIQUE(uid_imap, cuenta_hash, carpeta_imap)
            )
            """;

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private Mensaje desdeResultSet(ResultSet rs) throws SQLException {
        Mensaje m = new Mensaje(
                rs.getString("uid_imap"),
                rs.getString("remitente"),
                rs.getString("asunto"),
                rs.getString("cuerpo")
        );
        m.setUidImap(rs.getString("uid_imap"));
        m.setCarpetaImap(rs.getString("carpeta_imap"));
        m.setCategoria(rs.getString("categoria"));
        m.setPrioridad(rs.getString("prioridad"));
        m.setHtml(rs.getString("html"));
        return m;
    }

    private String obtenerFechaRecepcion(Mensaje m) {
        String fecha = null;
        try {
            fecha = m.getFechaRecepcion();
        } catch (Exception ignored) {
        }
        if (fecha == null || fecha.isBlank()) {
            return LocalDateTime.now().toString();
        }
        return fecha;
    }

    private int extraerAnio(String fechaRecepcion) {
        try {
            return LocalDateTime.parse(fechaRecepcion).getYear();
        } catch (DateTimeParseException e) {
            return LocalDateTime.now().getYear();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private record MensajeArchivado(
            long id,
            String uidImap,
            String cuentaHash,
            String carpetaImap,
            String remitente,
            String asunto,
            String cuerpo,
            String html,
            String categoria,
            String prioridad,
            String fechaRecepcion
    ) {
    }
}