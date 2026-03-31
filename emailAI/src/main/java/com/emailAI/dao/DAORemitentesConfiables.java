package com.emailAI.dao;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

// DAO para gestionar remitentes confiables usados al permitir imágenes externas.
public class DAORemitentesConfiables {

    private final String url; // URL de conexión SQLite para esta tabla.

    // Inicializa el DAO y crea la tabla de remitentes confiables si hace falta.
    public DAORemitentesConfiables(String url) {
        this.url = url;
        inicializarTabla();
    }

    // Crea la tabla que almacena los emails confiables.
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

    // Comprueba si un email ya está marcado como confiable.
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

    // Inserta un remitente en la lista confiable evitando duplicados.
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

    // Devuelve el conjunto de todos los remitentes confiables almacenados.
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