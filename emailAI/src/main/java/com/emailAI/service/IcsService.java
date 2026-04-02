package com.emailAI.service;

import com.emailAI.dao.DAOEventosCalendario;
import com.emailAI.dao.DAOEventosCalendario.Evento;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para procesar archivos .ics y sincronizar eventos con el calendario local.
 * Flujo: ICS -> Calendario local -> (opcional) Todoist como backup
 */
public class IcsService {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final DAOEventosCalendario daoEventos;
    
    public IcsService() throws Exception {
        this.daoEventos = new DAOEventosCalendario();
    }
    
    // Método público para acceder al DAO desde los tests
    public DAOEventosCalendario getDaoEventos() {
        return daoEventos;
    }
    
    /**
     * Procesa un archivo .ics y sincroniza los eventos con el calendario local
     */
    public List<Evento> procesarArchivoIcs(String rutaArchivo) throws Exception {
        Path path = Paths.get(rutaArchivo);
        if (!Files.exists(path)) {
            throw new IOException("El archivo ICS no existe: " + rutaArchivo);
        }
        
        List<Evento> eventos = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            StringBuilder eventoBuilder = new StringBuilder();
            String linea;
            boolean enEvento = false;
            
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                
                if (linea.equals("BEGIN:VEVENT")) {
                    enEvento = true;
                    eventoBuilder.setLength(0);
                    eventoBuilder.append(linea).append("\n");
                } else if (linea.equals("END:VEVENT")) {
                    eventoBuilder.append(linea).append("\n");
                    Evento evento = parsearEvento(eventoBuilder.toString());
                    if (evento != null) {
                        eventos.add(evento);
                    }
                    enEvento = false;
                } else if (enEvento) {
                    eventoBuilder.append(linea).append("\n");
                }
            }
        }
        
        // Guardar eventos en la base de datos local
        for (Evento evento : eventos) {
            daoEventos.guardarEvento(
                evento.fecha(), 
                evento.hora(), 
                evento.titulo(), 
                evento.detalle(), 
                "ics"
            );
        }
        
        return eventos;
    }
    
    /**
     * Parsea un evento individual del formato ICS
     */
    private Evento parsearEvento(String eventoTexto) {
        String dtStart = extraerCampo(eventoTexto, "DTSTART:");
        String dtEnd = extraerCampo(eventoTexto, "DTEND:");
        String summary = extraerCampo(eventoTexto, "SUMMARY:");
        String description = extraerCampo(eventoTexto, "DESCRIPTION:");
        
        if (dtStart == null || summary == null) {
            return null; // Evento inválido sin fecha o título
        }
        
        try {
            LocalDate fecha = parsearFecha(dtStart);
            LocalTime hora = parsearHora(dtStart);
            
            String detalle = description != null ? description : "";
            if (dtEnd != null) {
                LocalDate fechaFin = parsearFecha(dtEnd);
                LocalTime horaFin = parsearHora(dtEnd);
                detalle += String.format("\n(Fin: %s %s)", fechaFin, horaFin);
            }
            
            return new Evento(0, fecha, hora, summary, detalle, "ics");
            
        } catch (Exception e) {
            System.err.println("Error parseando evento: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae un campo específico del texto del evento
     */
    private String extraerCampo(String texto, String campo) {
        Pattern pattern = Pattern.compile(campo + "([^\\n]*)");
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            String valor = matcher.group(1);
            // Manejar líneas continuas (que comienzan con espacio)
            valor = valor.replaceAll("\\n[ ]+", " ");
            return valor.trim();
        }
        return null;
    }
    
    /**
     * Parsea una fecha del formato ICS
     */
    private LocalDate parsearFecha(String fechaStr) {
        if (fechaStr.length() >= 8) {
            String fechaPart = fechaStr.substring(0, 8);
            return LocalDate.parse(fechaPart, DATE_FORMAT);
        }
        throw new IllegalArgumentException("Formato de fecha inválido: " + fechaStr);
    }
    
    /**
     * Parsea una hora del formato ICS
     */
    private LocalTime parsearHora(String horaStr) {
        if (horaStr.length() >= 15 && horaStr.charAt(8) == 'T') {
            String horaPart = horaStr.substring(9, 15);
            return LocalTime.parse(horaPart, DateTimeFormatter.ofPattern("HHmmss"));
        }
        return null; // Sin hora específica
    }
    
    /**
     * Verifica si un archivo ICS ha sido modificado desde la última sincronización
     */
    public boolean haCambiadoDesdeUltimaSincronizacion(String rutaArchivo) {
        try {
            Path path = Paths.get(rutaArchivo);
            if (!Files.exists(path)) {
                return false;
            }
            
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            long lastSync = obtenerUltimaSincronizacion(rutaArchivo);
            
            return lastModified > lastSync;
        } catch (IOException e) {
            System.err.println("Error verificando cambios en archivo ICS: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Guarda el timestamp de la última sincronización
     */
    private void guardarUltimaSincronizacion(String rutaArchivo, long timestamp) {
        try {
            Path syncFile = Paths.get(rutaArchivo + ".sync");
            Files.writeString(syncFile, String.valueOf(timestamp));
        } catch (IOException e) {
            System.err.println("Error guardando timestamp de sincronización: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el timestamp de la última sincronización
     */
    private long obtenerUltimaSincronizacion(String rutaArchivo) {
        try {
            Path syncFile = Paths.get(rutaArchivo + ".sync");
            if (Files.exists(syncFile)) {
                String content = Files.readString(syncFile);
                return Long.parseLong(content.trim());
            }
        } catch (IOException | NumberFormatException e) {
            // Ignorar errores, retornar 0 para forzar sincronización
        }
        return 0;
    }
    
    /**
     * Sincroniza el archivo ICS si ha cambiado
     */
    public List<Evento> sincronizarSiEsNecesario(String rutaArchivo) {
        if (haCambiadoDesdeUltimaSincronizacion(rutaArchivo)) {
            try {
                List<Evento> eventos = procesarArchivoIcs(rutaArchivo);
                guardarUltimaSincronizacion(rutaArchivo, System.currentTimeMillis());
                System.out.println("Sincronizados " + eventos.size() + " eventos desde ICS");
                return eventos;
            } catch (Exception e) {
                System.err.println("Error sincronizando archivo ICS: " + e.getMessage());
            }
        }
        return List.of();
    }
    
    /**
     * Exporta todos los eventos del calendario local a un archivo .ics
     */
    public void exportarCalendarioAICS(String rutaArchivo) throws Exception {
        try {
            // Obtener todos los eventos de la base de datos
            List<Evento> eventos = daoEventos.listarEventos();
            
            if (eventos.isEmpty()) {
                throw new IOException("No hay eventos para exportar");
            }
            
            // Crear archivo ICS
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaArchivo))) {
                // Cabecera del archivo ICS
                writer.write("BEGIN:VCALENDAR\n");
                writer.write("VERSION:2.0\n");
                writer.write("PRODID:-//EmailAI//Calendario//EN\n");
                writer.write("CALSCALE:GREGORIAN\n");
                
                // Escribir cada evento
                for (Evento evento : eventos) {
                    writer.write("BEGIN:VEVENT\n");
                    
                    // UID único para el evento
                    writer.write("UID:" + evento.id() + "@emailai\n");
                    
                    // Fecha y hora de inicio
                    String dtStart = formatearFechaICS(evento.fecha(), evento.hora());
                    writer.write("DTSTART:" + dtStart + "\n");
                    
                    // Fecha y hora de fin (por defecto 1 hora después)
                    LocalTime horaFin = evento.hora() != null ? 
                        evento.hora().plusHours(1) : LocalTime.of(12, 0);
                    String dtEnd = formatearFechaICS(evento.fecha(), horaFin);
                    writer.write("DTEND:" + dtEnd + "\n");
                    
                    // Timestamp de creación
                    writer.write("DTSTAMP:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")) + "\n");
                    
                    // Título
                    writer.write("SUMMARY:" + escapeTextoICS(evento.titulo()) + "\n");
                    
                    // Descripción
                    if (evento.detalle() != null && !evento.detalle().isBlank()) {
                        writer.write("DESCRIPTION:" + escapeTextoICS(evento.detalle()) + "\n");
                    }
                    
                    writer.write("END:VEVENT\n");
                }
                
                // Cierre del archivo ICS
                writer.write("END:VCALENDAR\n");
            }
            
            System.out.println("Exportados " + eventos.size() + " eventos a " + rutaArchivo);
            
        } catch (IOException e) {
            throw new IOException("Error exportando calendario a ICS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Formatea fecha y hora para el estándar ICS
     */
    private String formatearFechaICS(LocalDate fecha, LocalTime hora) {
        if (hora == null) {
            hora = LocalTime.of(0, 0);
        }
        
        return fecha.format(DATE_FORMAT) + "T" + 
               hora.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
    }
    
    /**
     * Escapa caracteres especiales para texto en formato ICS
     */
    private String escapeTextoICS(String texto) {
        if (texto == null) return "";
        
        return texto.replace("\\", "\\\\")
                  .replace(",", "\\,")
                  .replace(";", "\\;")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
