package com.emailAI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio centralizado para manejo de excepciones con logging estructurado.
 * Reemplaza System.err.println() y e.printStackTrace() por logging profesional.
 */
public class ExceptionHandlerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerService.class);
    
    /**
     * Maneja excepciones de forma estructurada con diferentes niveles de log.
     */
    public static void handleException(String contexto, Exception e) {
        if (e == null) {
            logger.warn("Excepción nula en contexto: {}", contexto);
            return;
        }
        
        // Determinar nivel de log basado en tipo de excepción
        if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
            logger.error("Error de conexión en {}: {}", contexto, e.getMessage());
        } else if (e instanceof java.io.IOException) {
            logger.error("Error de I/O en {}: {}", contexto, e.getMessage());
        } else if (e instanceof java.sql.SQLException) {
            logger.error("Error de base de datos en {}: {}", contexto, e.getMessage());
        } else if (e instanceof IllegalArgumentException) {
            logger.warn("Parámetro inválido en {}: {}", contexto, e.getMessage());
        } else if (e instanceof SecurityException) {
            logger.error("Error de seguridad en {}: {}", contexto, e.getMessage());
        } else {
            logger.error("Error inesperado en {}: {}", contexto, e.getMessage(), e);
        }
    }
    
    /**
     * Maneja excepciones con contexto adicional (ej. usuario, operación específica).
     */
    public static void handleException(String contexto, String detalles, Exception e) {
        if (e == null) {
            logger.warn("Excepción nula en contexto: {} - {}", contexto, detalles);
            return;
        }
        
        String mensajeCompleto = detalles.isBlank() ? contexto : contexto + " - " + detalles;
        handleException(mensajeCompleto, e);
    }
    
    /**
     * Registra advertencias con contexto.
     */
    public static void logWarning(String contexto, String mensaje) {
        logger.warn("Advertencia en {}: {}", contexto, mensaje);
    }
    
    /**
     * Registra errores con contexto sin excepción.
     */
    public static void logError(String contexto, String mensaje) {
        logger.error("Error en {}: {}", contexto, mensaje);
    }
    
    /**
     * Registra información de depuración.
     */
    public static void logDebug(String contexto, String mensaje) {
        logger.debug("Debug en {}: {}", contexto, mensaje);
    }
    
    /**
     * Registra operaciones importantes con contexto.
     */
    public static void logInfo(String contexto, String mensaje) {
        logger.info("Info en {}: {}", contexto, mensaje);
    }
    
    /**
     * Crea un mensaje de error amigable para el usuario.
     */
    public static String crearMensajeUsuario(Exception e) {
        if (e == null) {
            return "Error desconocido";
        }
        
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "Error sin descripción";
        }
        
        // Mensajes amigables para errores comunes
        if (e instanceof java.net.ConnectException) {
            return "No se puede conectar al servidor. Verifique su conexión a internet.";
        } else if (e instanceof java.net.SocketTimeoutException) {
            return "Tiempo de espera agotado. Intente nuevamente.";
        } else if (e instanceof java.io.FileNotFoundException) {
            return "Archivo no encontrado. Verifique la ruta especificada.";
        } else if (e instanceof java.sql.SQLException) {
            return "Error en la base de datos. Contacte al administrador.";
        } else if (mensaje.contains("Access denied")) {
            return "Acceso denegado. Verifique sus credenciales.";
        } else if (mensaje.contains("authentication failed")) {
            return "Autenticación fallida. Usuario o contraseña incorrectos.";
        }
        
        return mensaje;
    }
    
    /**
     * Verifica si una excepción es crítica (requiere intervención inmediata).
     */
    public static boolean esCritica(Throwable e) {
        if (e == null) return false;
        
        return e instanceof java.lang.OutOfMemoryError ||
               e instanceof java.lang.StackOverflowError ||
               e instanceof java.lang.SecurityException ||
               e.getMessage() != null && e.getMessage().contains("Access denied");
    }
    
    /**
     * Recupera información de la excepción para debugging.
     */
    public static String obtenerInfoDepuracion(Exception e) {
        if (e == null) return "Excepción nula";
        
        StringBuilder info = new StringBuilder();
        info.append("Tipo: ").append(e.getClass().getSimpleName());
        info.append(", Mensaje: ").append(e.getMessage());
        
        if (e.getCause() != null) {
            info.append(", Causa: ").append(e.getCause().getMessage());
        }
        
        StackTraceElement[] stack = e.getStackTrace();
        if (stack != null && stack.length > 0) {
            info.append(", Origen: ").append(stack[0].toString());
        }
        
        return info.toString();
    }
}
