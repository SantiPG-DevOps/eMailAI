package com.emailAI.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO de cuentas de correo → correos.db
public class DAOCuentas {

    public record CuentaGuardada(String email, String servidor, int puerto,
                                  String usuarioCifrado, String passwordCifrada,
                                  boolean esDefault) {}

    public DAOCuentas() {
        crearTablaSiNoExiste();
    }

    /** @deprecated Usar constructor sin argumentos. */
    @Deprecated
    public DAOCuentas(String url) {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() {
        String sql = """
                CREATE TABLE IF NOT EXISTS cuentas (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    email            TEXT NOT NULL UNIQUE,
                    servidor         TEXT,
                    puerto           INTEGER,
                    usuario_cifrado  TEXT,
                    password_cifrada TEXT,
                    es_default       INTEGER DEFAULT 0
                )
                """;
        try (Connection conn = ConexionBD.getConnectionCorreos();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void guardar(CuentaGuardada c) {
        String sql = """
                INSERT INTO cuentas(email, servidor, puerto, usuario_cifrado, password_cifrada, es_default)
                VALUES(?, ?, ?, ?, ?, ?)
                ON CONFLICT(email) DO UPDATE SET
                    servidor         = excluded.servidor,
                    puerto           = excluded.puerto,
                    usuario_cifrado  = excluded.usuario_cifrado,
                    password_cifrada = excluded.password_cifrada,
                    es_default       = excluded.es_default
                """;
        try (Connection conn = ConexionBD.getConnectionCorreos();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.email());
            ps.setString(2, c.servidor());
            ps.setInt(3, c.puerto());
            ps.setString(4, c.usuarioCifrado());
            ps.setString(5, c.passwordCifrada());
            ps.setInt(6, c.esDefault() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<CuentaGuardada> listarTodas() {
        List<CuentaGuardada> lista = new ArrayList<>();
        String sql = """
                SELECT email, servidor, puerto, usuario_cifrado, password_cifrada, es_default
                FROM cuentas
                """;
        try (Connection conn = ConexionBD.getConnectionCorreos();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new CuentaGuardada(
                        rs.getString("email"),
                        rs.getString("servidor"),
                        rs.getInt("puerto"),
                        rs.getString("usuario_cifrado"),
                        rs.getString("password_cifrada"),
                        rs.getInt("es_default") == 1
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public void eliminar(String email) {
        String sql = "DELETE FROM cuentas WHERE email = ?";
        try (Connection conn = ConexionBD.getConnectionCorreos();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}