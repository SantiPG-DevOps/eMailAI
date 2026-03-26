package com.emailAI.dao;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DAORemitentesConfiables {

    private final String url;

    public DAORemitentesConfiables(String url) {
        this.url = url;
        inicializarTabla();
    }

    private void inicializarTabla() {
        String sql = """
                CREATE TABLE IF NOT EXISTS remitentes_confiables (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    email TEXT UNIQUE NOT NULL
                )
                """;
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean esConfiable(String email) {
        if (email == null) return false;
        String sql = "SELECT 1 FROM remitentes_confiables WHERE email = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void marcarConfiable(String email) {
        if (email == null) return;
        String sql = "INSERT OR IGNORE INTO remitentes_confiables(email) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<String> listarTodos() {
        Set<String> res = new HashSet<>();
        String sql = "SELECT email FROM remitentes_confiables";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }
}