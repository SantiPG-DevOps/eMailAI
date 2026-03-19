package com.emailAI.dao;

import com.emailAI.security.UtilidadCifrado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOContactos {

    public static class Contacto {
        private final int id;
        private final int cuentaId;
        private final String nombre;
        private final String email;
        private final String telefono;
        private final String notas;

        public Contacto(int id, int cuentaId, String nombre, String email, String telefono, String notas) {
            this.id = id;
            this.cuentaId = cuentaId;
            this.nombre = nombre;
            this.email = email;
            this.telefono = telefono;
            this.notas = notas;
        }

        public int getId() { return id; }
        public int getCuentaId() { return cuentaId; }
        public String getNombre() { return nombre; }
        public String getEmail() { return email; }
        public String getTelefono() { return telefono; }
        public String getNotas() { return notas; }

        @Override
        public String toString() {
            return nombre + " <" + email + ">";
        }
    }

    private final String claveCifrado; // derivada de la pass de la cuenta

    public DAOContactos(String claveCifrado) throws Exception {
        this.claveCifrado = claveCifrado;
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS contactos (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    cuenta_id INTEGER NOT NULL,
                    nombre    TEXT NOT NULL,
                    email     TEXT NOT NULL,
                    telefono  TEXT,
                    notas     TEXT,
                    FOREIGN KEY (cuenta_id) REFERENCES cuentas(id)
                )
                """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.executeUpdate();
        }
    }

    public void insertarContacto(int cuentaId, String nombre, String email,
                                 String telefono, String notas) throws Exception {
        String sql = "INSERT INTO contactos(cuenta_id, nombre, email, telefono, notas) VALUES(?,?,?,?,?)";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, cuentaId);
            st.setString(2, UtilidadCifrado.cifrar(nombre, claveCifrado));
            st.setString(3, UtilidadCifrado.cifrar(email, claveCifrado));
            st.setString(4, telefono != null && !telefono.isEmpty()
                    ? UtilidadCifrado.cifrar(telefono, claveCifrado) : null);
            st.setString(5, notas != null && !notas.isEmpty()
                    ? UtilidadCifrado.cifrar(notas, claveCifrado) : null);

            st.executeUpdate();
        }
    }

    public List<Contacto> listarPorCuenta(int cuentaId) throws Exception {
        String sql = "SELECT id, cuenta_id, nombre, email, telefono, notas FROM contactos WHERE cuenta_id = ? ORDER BY nombre";
        List<Contacto> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, cuentaId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int cid = rs.getInt("cuenta_id");
                    String nombre = UtilidadCifrado.descifrar(rs.getString("nombre"), claveCifrado);
                    String email = UtilidadCifrado.descifrar(rs.getString("email"), claveCifrado);
                    String telCif = rs.getString("telefono");
                    String notasCif = rs.getString("notas");

                    String telefono = telCif != null ? UtilidadCifrado.descifrar(telCif, claveCifrado) : "";
                    String notas = notasCif != null ? UtilidadCifrado.descifrar(notasCif, claveCifrado) : "";

                    lista.add(new Contacto(id, cid, nombre, email, telefono, notas));
                }
            }
        }
        return lista;
    }

    public void borrarContacto(int id) throws Exception {
        String sql = "DELETE FROM contactos WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }
}
