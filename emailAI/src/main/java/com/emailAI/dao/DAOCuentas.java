package com.emailAI.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOCuentas {

    public static class CuentaGuardada {
        public int id;
        public String servidorImap;
        public String servidorSmtp;
        public String emailCifrado;
        public String passMaestraHash;
        public String passCorreoCifrada;   // NUEVO

        public CuentaGuardada(int id,
                              String servidorImap,
                              String servidorSmtp,
                              String emailCifrado,
                              String passMaestraHash,
                              String passCorreoCifrada) {
            this.id = id;
            this.servidorImap = servidorImap;
            this.servidorSmtp = servidorSmtp;
            this.emailCifrado = emailCifrado;
            this.passMaestraHash = passMaestraHash;
            this.passCorreoCifrada = passCorreoCifrada;
        }
    }

    public DAOCuentas() throws SQLException {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS cuentas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    servidor_imap       TEXT NOT NULL,
                    servidor_smtp       TEXT NOT NULL,
                    email_cifrado       TEXT NOT NULL UNIQUE,
                    pass_maestra_hash   TEXT NOT NULL,
                    pass_correo_cifrada TEXT NOT NULL
                );
                """;

        try (Connection conn = ConexionBD.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public void guardarCuenta(String servidorImap,
                              String servidorSmtp,
                              String emailCifrado,
                              String passMaestraHash,
                              String passCorreoCifrada) throws SQLException {

        String sql = """
                INSERT INTO cuentas(
                    servidor_imap,
                    servidor_smtp,
                    email_cifrado,
                    pass_maestra_hash,
                    pass_correo_cifrada
                )
                VALUES (?, ?, ?, ?, ?);
                """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, servidorImap);
            ps.setString(2, servidorSmtp);
            ps.setString(3, emailCifrado);
            ps.setString(4, passMaestraHash);
            ps.setString(5, passCorreoCifrada);
            ps.executeUpdate();
        }
    }

    public List<CuentaGuardada> listarCuentas() throws SQLException {
        String sql = """
                SELECT
                    id,
                    servidor_imap,
                    servidor_smtp,
                    email_cifrado,
                    pass_maestra_hash,
                    pass_correo_cifrada
                FROM cuentas
                """;

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
                        rs.getString("pass_maestra_hash"),
                        rs.getString("pass_correo_cifrada")
                ));
            }
        }

        return lista;
    }

    // Para AppConsole antigua
    public ResultSet obtenerPrimeraCuenta(Connection conn) throws SQLException {
        String sql = "SELECT * FROM cuentas LIMIT 1";
        PreparedStatement ps = conn.prepareStatement(sql);
        return ps.executeQuery();
    }
}
