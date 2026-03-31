package com.emailAI.dao;

import com.emailAI.model.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO para almacenar y recuperar ejemplos etiquetados usados en entrenamiento de modelos IA.
public class DAOEntrenamiento {

    // DTO mutable que representa un ejemplo de entrenamiento persistido.
    public static class Ejemplo {
        public String remitente;
        public String asunto;
        public String cuerpo;
        public String etiqueta;    // SPAM, LEGITIMO, NORMAL o URGENTE
        public String tipoModelo;  // "SPAM" o "PRIORIDAD"

        public Ejemplo(String remitente, String asunto, String cuerpo,
                       String etiqueta, String tipoModelo) {
            this.remitente = remitente;
            this.asunto = asunto;
            this.cuerpo = cuerpo;
            this.etiqueta = etiqueta;
            this.tipoModelo = tipoModelo;
        }
    }

    // Inicializa el DAO asegurando la tabla de ejemplos.
    public DAOEntrenamiento() throws SQLException {
        crearTablaSiNoExiste();
    }

    // Crea la tabla de ejemplos de entrenamiento si no existe.
    private void crearTablaSiNoExiste() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS ejemplos_entrenamiento (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    remitente   TEXT,
                    asunto      TEXT,
                    cuerpo      TEXT,
                    etiqueta    TEXT,
                    tipo_modelo TEXT DEFAULT 'SPAM'
                );
                """;
        try (Connection conn = ConexionBD.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    // Guarda un ejemplo etiquetado para un tipo de modelo concreto.
    public void guardarEjemplo(Mensaje mensaje, String etiqueta, String tipoModelo) throws SQLException {
        String sql = """
                INSERT INTO ejemplos_entrenamiento(remitente, asunto, cuerpo, etiqueta, tipo_modelo)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, mensaje.getRemitente());
            ps.setString(2, mensaje.getAsunto());
            ps.setString(3, mensaje.getCuerpo());
            ps.setString(4, etiqueta);
            ps.setString(5, tipoModelo);
            ps.executeUpdate();
        }
    }

    /**
     * Número de interacciones no-SPAM con un remitente (para atributo es_remitente_frecuente).
     */
    // Cuenta interacciones no-spam de un remitente para señales de prioridad/confianza.
    public int contarInteracciones(String remitente) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM ejemplos_entrenamiento
                 WHERE remitente = ?
                   AND etiqueta != 'SPAM'
                """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, remitente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Lista todos los ejemplos de un tipo de modelo (SPAM, PRIORIDAD, etc.).
     */
    // Lista todos los ejemplos pertenecientes a un tipo de modelo.
    public List<Ejemplo> listarEjemplosPorTipo(String tipoModelo) throws SQLException {
        String sql = """
                SELECT remitente, asunto, cuerpo, etiqueta, tipo_modelo
                  FROM ejemplos_entrenamiento
                 WHERE tipo_modelo = ?
                """;

        List<Ejemplo> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipoModelo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Ejemplo(
                            rs.getString("remitente"),
                            rs.getString("asunto"),
                            rs.getString("cuerpo"),
                            rs.getString("etiqueta"),
                            rs.getString("tipo_modelo")
                    ));
                }
            }
        }
        return lista;
    }

    /**
     * Compatibilidad: devuelve los ejemplos del modelo SPAM por defecto.
     */
    // Método de compatibilidad que devuelve los ejemplos del modelo SPAM.
    public List<Ejemplo> listarEjemplos() throws SQLException {
        return listarEjemplosPorTipo("SPAM");
    }
}
