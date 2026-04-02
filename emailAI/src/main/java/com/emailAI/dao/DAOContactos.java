package com.emailAI.dao;

import com.emailAI.model.Contacto;
import com.emailAI.security.UtilidadCifrado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO de contactos → contactos.db
public class DAOContactos {

    public DAOContactos() {
        inicializarTabla();
    }

    /** @deprecated Usar constructor sin argumentos. */
    @Deprecated
    public DAOContactos(String url) {
        inicializarTabla();
    }

    private void inicializarTabla() {
        String sql = """
                CREATE TABLE IF NOT EXISTS contactos (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre            TEXT NOT NULL,
                    apellido_cifrado  TEXT,
                    email_cifrado     TEXT,
                    telefono_cifrado  TEXT,
                    notas_cifrado     TEXT
                )
                """;
        try (Connection conn = ConexionBD.getConnectionContactos();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Contacto> listarTodos() {
        List<Contacto> lista = new ArrayList<>();
        String sql = """
                SELECT id, nombre, apellido_cifrado, email_cifrado, telefono_cifrado, notas_cifrado
                FROM contactos
                """;
        try (Connection conn = ConexionBD.getConnectionContactos();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Contacto(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        descifrarSeguro(rs.getString("apellido_cifrado")),
                        descifrarSeguro(rs.getString("email_cifrado")),
                        descifrarSeguro(rs.getString("telefono_cifrado")),
                        descifrarSeguro(rs.getString("notas_cifrado"))
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public void guardarOActualizar(Contacto c) {
        if (c.getId() == null) insertar(c);
        else actualizar(c);
    }

    private void insertar(Contacto c) {
        String sql = """
                INSERT INTO contactos (nombre, apellido_cifrado, email_cifrado, telefono_cifrado, notas_cifrado)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConexionBD.getConnectionContactos();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, cifrarSeguro(c.getApellido()));
            ps.setString(3, cifrarSeguro(c.getEmail()));
            ps.setString(4, cifrarSeguro(c.getTelefono()));
            ps.setString(5, cifrarSeguro(c.getNotas()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getInt(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void actualizar(Contacto c) {
        String sql = """
                UPDATE contactos
                SET nombre = ?, apellido_cifrado = ?, email_cifrado = ?,
                    telefono_cifrado = ?, notas_cifrado = ?
                WHERE id = ?
                """;
        try (Connection conn = ConexionBD.getConnectionContactos();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, cifrarSeguro(c.getApellido()));
            ps.setString(3, cifrarSeguro(c.getEmail()));
            ps.setString(4, cifrarSeguro(c.getTelefono()));
            ps.setString(5, cifrarSeguro(c.getNotas()));
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void borrar(Contacto c) {
        if (c.getId() == null) return;
        String sql = "DELETE FROM contactos WHERE id = ?";
        try (Connection conn = ConexionBD.getConnectionContactos();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private String cifrarSeguro(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try { return UtilidadCifrado.cifrar(texto); }
        catch (Exception e) { e.printStackTrace(); return null; }
    }

    private String descifrarSeguro(String textoCif) {
        if (textoCif == null || textoCif.isBlank()) return "";
        try { return UtilidadCifrado.descifrar(textoCif); }
        catch (Exception e) { e.printStackTrace(); return ""; }
    }
}