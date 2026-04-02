package com.emailAI.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO para la tabla de cuentas de correo almacenadas localmente (servidores, email cifrado y hash).
public class DAOCuentas {

    // Representa una fila de la tabla cuentas con los datos mínimos para login.
    public static class CuentaGuardada {
        public int id;
        public String servidorImap;
        public String servidorSmtp;
        public String emailCifrado;
        public String passMaestraHash;

        public CuentaGuardada(int id,
                              String servidorImap,
                              String servidorSmtp,
                              String emailCifrado,
                              String passMaestraHash) {
            this.id = id;
            this.servidorImap = servidorImap;
            this.servidorSmtp = servidorSmtp;
            this.emailCifrado = emailCifrado;
            this.passMaestraHash = passMaestraHash;
        }
    }

    // Crea la tabla de cuentas si no existe aún.
    public DAOCuentas() throws SQLException {
        crearTablaSiNoExiste();
    }

    // Ejecuta el DDL necesario para tener la tabla de cuentas preparada.
    private void crearTablaSiNoExiste() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS cuentas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    servidor_imap      TEXT NOT NULL,
                    servidor_smtp      TEXT NOT NULL,
                    email_cifrado      TEXT NOT NULL UNIQUE,
                    pass_maestra_hash  TEXT NOT NULL
                );
                """;

        try (Connection conn = ConexionBD.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    // Inserta una nueva cuenta con servidores, email cifrado y hash de contraseña maestra.
    public void guardarCuenta(String servidorImap,
                              String servidorSmtp,
                              String emailCifrado,
                              String passMaestraHash) throws SQLException {

        String sql = """
                INSERT INTO cuentas(servidor_imap, servidor_smtp, email_cifrado, pass_maestra_hash)
                VALUES (?, ?, ?, ?);
                """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, servidorImap);
            ps.setString(2, servidorSmtp);
            ps.setString(3, emailCifrado);
            ps.setString(4, passMaestraHash);
            ps.executeUpdate();
        }
    }

    // Recupera todas las cuentas guardadas en forma de lista utilizable por la UI.
    public List<CuentaGuardada> listarCuentas() throws SQLException {
        String sql = "SELECT id, servidor_imap, servidor_smtp, email_cifrado, pass_maestra_hash FROM cuentas";
        List<CuentaGuardada> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new CuentaGuardada(
                        rs.getInt("id"),
                        rs.getString("servidor_imap"),
                        rs.getString("servidor_smtp"),
                        rs.getString("email_cifrado"),
                        rs.getString("pass_maestra_hash")
                ));
            }
        }

        return lista;
    }

    // Para AppConsole antigua
    // Método legado para la consola antigua que devuelve solo la primera cuenta.
    public ResultSet obtenerPrimeraCuenta(Connection conn) throws SQLException {
        String sql = "SELECT * FROM cuentas LIMIT 1";
        PreparedStatement ps = conn.prepareStatement(sql);
        return ps.executeQuery();
    }
    
    // Elimina una cuenta a partir de su id primario.
    public void eliminarCuentaPorId(int id) throws SQLException {
        String sql = "DELETE FROM cuentas WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
