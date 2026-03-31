package com.emailAI.dao;

import com.emailAI.model.Contacto;
import com.emailAI.security.UtilidadCifrado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO de contactos: persistencia cifrada de datos personales y operaciones CRUD básicas.
public class DAOContactos {

    private final String url; // URL de conexión SQLite usada por este DAO.

    // Inicializa el DAO con su URL y asegura la tabla requerida.
    public DAOContactos(String url) {
        this.url = url;
        inicializarTabla();
    }

    // Crea la tabla de contactos si todavía no existe.
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

    // Recupera todos los contactos y descifra los campos almacenados.
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

    // Inserta o actualiza según si el contacto tiene id asignado.
    public void guardarOActualizar(Contacto c) {
        if (c.getId() == null) {
            insertar(c);
        } else {
            actualizar(c);
        }
    }

    // Inserta un nuevo contacto y actualiza su id generado.
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

    // Actualiza un contacto existente identificado por su id.
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

    // Borra un contacto existente por id.
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

    // Cifra un texto si existe, devolviendo null ante error o vacío.
    private String cifrarSeguro(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return UtilidadCifrado.cifrar(texto);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Descifra un texto cifrado de forma segura devolviendo vacío si falla.
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