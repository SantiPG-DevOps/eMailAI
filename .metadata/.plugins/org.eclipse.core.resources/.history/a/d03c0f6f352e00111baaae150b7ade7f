package com.emailAI.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Utilidad central para abrir conexiones JDBC a la base de datos SQLite local.
public class ConexionBD {

    private static final String URL = "jdbc:sqlite:emailAI.db"; // Ruta JDBC de la BD del proyecto.

    // Devuelve una conexión nueva a la base local.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
