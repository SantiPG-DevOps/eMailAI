package com.emailAI.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona las conexiones SQLite del proyecto.
 *
 * Estructura:
 *   ~/.emailAI/db/correos.db
 *   ~/.emailAI/db/agenda.db
 *   ~/.emailAI/db/contactos.db
 *   ~/.emailAI/db/ia.db
 *   ~/.emailAI/db/archivo/correos_YYYY.db
 */
public class ConexionBD {

    private static final Path BASE_DIR = Path.of(System.getProperty("user.home"), ".emailAI", "db");
    private static final Path ARCHIVO_DIR = BASE_DIR.resolve("archivo");

    private static final Path CORREOS_DB   = BASE_DIR.resolve("correos.db");
    private static final Path AGENDA_DB    = BASE_DIR.resolve("agenda.db");
    private static final Path CONTACTOS_DB = BASE_DIR.resolve("contactos.db");
    private static final Path IA_DB        = BASE_DIR.resolve("ia.db");

    static {
        try {
            Files.createDirectories(BASE_DIR);
            Files.createDirectories(ARCHIVO_DIR);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("No se pudieron crear los directorios de bases de datos: " + e.getMessage());
        }
    }

    public static Connection getConnectionCorreos() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + CORREOS_DB.toAbsolutePath());
    }

    public static Connection getConnectionAgenda() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + AGENDA_DB.toAbsolutePath());
    }

    public static Connection getConnectionContactos() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + CONTACTOS_DB.toAbsolutePath());
    }

    public static Connection getConnectionIA() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + IA_DB.toAbsolutePath());
    }

    public static Connection getConnectionArchivoCorreos(int anio) throws SQLException {
        Path archivoAnual = ARCHIVO_DIR.resolve("correos_" + anio + ".db");
        return DriverManager.getConnection("jdbc:sqlite:" + archivoAnual.toAbsolutePath());
    }

    public static Path getRutaCorreos() {
        return CORREOS_DB;
    }

    public static Path getRutaAgenda() {
        return AGENDA_DB;
    }

    public static Path getRutaContactos() {
        return CONTACTOS_DB;
    }

    public static Path getRutaIA() {
        return IA_DB;
    }

    public static Path getRutaArchivoCorreos(int anio) {
        return ARCHIVO_DIR.resolve("correos_" + anio + ".db");
    }

    /**
     * @deprecated Usar el método específico según la BD.
     */
    @Deprecated
    public static Connection getConnection() throws SQLException {
        return getConnectionCorreos();
    }
}