package com.emailAI.dao;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

// DAO de remitentes confiables → ia.db
public class DAORemitentesConfiables {

    public DAORemitentesConfiables() throws SQLException {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS remitentes_confiables (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    remitente TEXT NOT NULL UNIQUE
                )
                """;
        try (Connection conn = ConexionBD.getConnectionIA();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public void agregar(String remitente) throws SQLException {
        String sql = "INSERT OR IGNORE INTO remitentes_confiables(remitente) VALUES(?)";
        try (Connection conn = ConexionBD.getConnectionIA();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remitente);
            ps.executeUpdate();
        }
    }

    public void eliminar(String remitente) throws SQLException {
        String sql = "DELETE FROM remitentes_confiables WHERE remitente = ?";
        try (Connection conn = ConexionBD.getConnectionIA();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remitente);
            ps.executeUpdate();
        }
    }

    public Set<String> listarTodos() throws SQLException {
        Set<String> set = new HashSet<>();
        String sql = "SELECT remitente FROM remitentes_confiables";
        try (Connection conn = ConexionBD.getConnectionIA();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) set.add(rs.getString("remitente"));
        }
        return set;
    }

    public boolean esConfiable(String remitente) throws SQLException {
        String sql = "SELECT 1 FROM remitentes_confiables WHERE remitente = ?";
        try (Connection conn = ConexionBD.getConnectionIA();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remitente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}