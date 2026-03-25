package com.emailAI.dao;

import com.emailAI.model.Contacto;
import com.emailAI.security.UtilidadCifrado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOContactos {

    private final String url;

    public DAOContactos(String url) {
        this.url = url;
        inicializarTabla();
    }

    private void inicializarTabla() {
        String sql = """
                CREATE TABLE IF NOT EXISTS contactos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    email_cifrado TEXT,
                    telefono_cifrado TEXT,
                    notas_cifrado TEXT
                )
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Contacto> listarTodos() {
        List<Contacto> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, email_cifrado, telefono_cifrado, notas_cifrado FROM contactos ORDER BY nombre ASC";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                String emailCif = rs.getString("email_cifrado");
                String telCif   = rs.getString("telefono_cifrado");
                String notasCif = rs.getString("notas_cifrado");

                String email = descifrarSeguro(emailCif);
                String tel   = descifrarSeguro(telCif);
                String notas = descifrarSeguro(notasCif);

                lista.add(new Contacto(id, nombre, email, tel, notas));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public void guardarOActualizar(Contacto c) {
        if (c.getId() == null) {
            insertar(c);
        } else {
            actualizar(c);
        }
    }

    private void insertar(Contacto c) {
        String sql = """
                INSERT INTO contactos (nombre, email_cifrado, telefono_cifrado, notas_cifrado)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getNombre());
            ps.setString(2, cifrarSeguro(c.getEmail()));
            ps.setString(3, cifrarSeguro(c.getTelefono()));
            ps.setString(4, cifrarSeguro(c.getNotas()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void actualizar(Contacto c) {
        String sql = """
                UPDATE contactos
                SET nombre = ?, email_cifrado = ?, telefono_cifrado = ?, notas_cifrado = ?
                WHERE id = ?
                """;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNombre());
            ps.setString(2, cifrarSeguro(c.getEmail()));
            ps.setString(3, cifrarSeguro(c.getTelefono()));
            ps.setString(4, cifrarSeguro(c.getNotas()));
            ps.setInt(5, c.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void borrar(Contacto c) {
        if (c.getId() == null) return;

        String sql = "DELETE FROM contactos WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String cifrarSeguro(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return UtilidadCifrado.cifrar(texto);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String descifrarSeguro(String textoCif) {
        if (textoCif == null || textoCif.isBlank()) return "";
        try {
            return UtilidadCifrado.descifrar(textoCif);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}