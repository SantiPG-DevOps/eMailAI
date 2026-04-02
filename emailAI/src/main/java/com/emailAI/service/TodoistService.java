package com.emailAI.service;

import com.emailAI.dao.DAOTareas;
import com.emailAI.model.Tarea;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente para la API de Todoist.
 * Permite sincronizar tareas locales con Todoist como backup.
 */
public class TodoistService {
    
    private static final String TODOIST_API_BASE = "https://api.todoist.com/rest/v2";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final String apiKey;
    private final DAOTareas daoTareas;
    private final HttpClient httpClient;
    
    public TodoistService(String apiKey) throws Exception {
        this.apiKey = apiKey;
        this.daoTareas = new DAOTareas();
        this.httpClient = HttpClient.newHttpClient();
    }
    
    /**
     * Verifica si la API key es válida haciendo una llamada simple
     */
    public boolean verificarApiKey() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TODOIST_API_BASE + "/projects"))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
                
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error verificando API key de Todoist: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Sincroniza todas las tareas locales con Todoist
     */
    public List<String> sincronizarTareasConTodoist() throws Exception {
        List<String> errores = new ArrayList<>();
        
        try {
            // Obtener tareas locales
            List<Tarea> tareasLocales = daoTareas.listarTodas();
            
            // Obtener tareas de Todoist
            List<TodoistTask> tareasTodoist = obtenerTareasDesdeTodoist();
            
            // Sincronizar tareas locales -> Todoist
            for (Tarea tareaLocal : tareasLocales) {
                try {
                    if (!tareaLocal.getEstado().equals("COMPLETADA")) {
                        String todoistId = crearTareaEnTodoist(tareaLocal);
                        if (todoistId != null) {
                            // Guardar ID de Todoist en la tarea local (necesitaríamos modificar el modelo)
                            System.out.println("Tarea sincronizada con Todoist: " + tareaLocal.getTitulo());
                        }
                    }
                } catch (Exception e) {
                    errores.add("Error sincronizando tarea '" + tareaLocal.getTitulo() + "': " + e.getMessage());
                }
            }
            
            // Sincronizar tareas Todoist -> Local (para backup)
            for (TodoistTask tareaTodoist : tareasTodoist) {
                if (!existeTareaLocal(tareaTodoist)) {
                    try {
                        Tarea tareaLocal = convertirTareaTodoistALocal(tareaTodoist);
                        daoTareas.guardarOActualizar(tareaLocal);
                        System.out.println("Tarea backup desde Todoist: " + tareaTodoist.content);
                    } catch (Exception e) {
                        errores.add("Error creando backup de tarea '" + tareaTodoist.content + "': " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            errores.add("Error general en sincronización: " + e.getMessage());
        }
        
        return errores;
    }
    
    /**
     * Crea una tarea en Todoist y retorna su ID
     */
    private String crearTareaEnTodoist(Tarea tarea) throws Exception {
        String json = String.format("""
            {
                "content": "%s",
                "description": "%s",
                "due_date": "%s",
                "priority": %d
            }
            """,
            escaparJson(tarea.getTitulo()),
            escaparJson(tarea.getDescripcion() != null ? tarea.getDescripcion() : ""),
            tarea.getFechaVencimiento() != null ? tarea.getFechaVencimiento().format(DATE_FORMATTER) : "",
            convertirPrioridadTodoist(tarea.getPrioridad())
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TODOIST_API_BASE + "/tasks"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
            
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
            
        if (response.statusCode() == 200) {
            // Éxito en la creación
            return "created";
        } else {
            throw new IOException("Error creando tarea en Todoist: " + response.statusCode() + " " + response.body());
        }
    }
    
    /**
     * Obtiene todas las tareas desde Todoist
     */
    private List<TodoistTask> obtenerTareasDesdeTodoist() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TODOIST_API_BASE + "/tasks"))
            .header("Authorization", "Bearer " + apiKey)
            .GET()
            .build();
            
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
            
        if (response.statusCode() == 200) {
            return parsearTareasDesdeJson(response.body());
        } else {
            throw new IOException("Error obteniendo tareas de Todoist: " + response.statusCode());
        }
    }
    
    /**
     * Parsea la respuesta JSON de Todoist
     */
    private List<TodoistTask> parsearTareasDesdeJson(String json) {
        List<TodoistTask> tareas = new ArrayList<>();
        
        // Parseo simple (en producción usar una librería JSON)
        String[] lines = json.split("\\{");
        for (String line : lines) {
            if (line.contains("\"content\"")) {
                TodoistTask task = new TodoistTask();
                task.content = extraerCampoJson(line, "content");
                task.due = extraerCampoJson(line, "due_date");
                task.priority = extraerCampoJson(line, "priority");
                tareas.add(task);
            }
        }
        
        return tareas;
    }
    
    /**
     * Extrae un campo de una línea JSON simple
     */
    private String extraerCampoJson(String line, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    /**
     * Convierte una tarea de Todoist a modelo local
     */
    private Tarea convertirTareaTodoistALocal(TodoistTask task) {
        LocalDate fechaVencimiento = null;
        if (task.due != null && !task.due.isBlank()) {
            try {
                fechaVencimiento = LocalDate.parse(task.due, DATE_FORMATTER);
            } catch (Exception e) {
                // Ignorar error de parseo de fecha
            }
        }
        
        return new Tarea(
            null, // ID local
            task.content,
            "", // Descripción
            fechaVencimiento,
            "PENDIENTE",
            "", // Etiquetas
            convertirPrioridadDesdeTodoist(task.priority)
        );
    }
    
    /**
     * Verifica si una tarea ya existe localmente (por título)
     */
    private boolean existeTareaLocal(TodoistTask task) {
        try {
            List<Tarea> tareasLocales = daoTareas.listarTodas();
            return tareasLocales.stream()
                .anyMatch(t -> t.getTitulo().equals(task.content));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Convierte prioridad local a formato Todoist (1-4)
     */
    private int convertirPrioridadTodoist(String prioridadLocal) {
        if (prioridadLocal == null) return 1;
        return switch (prioridadLocal.toUpperCase()) {
            case "ALTA" -> 4;
            case "MEDIA" -> 2;
            case "BAJA" -> 1;
            default -> 1;
        };
    }
    
    /**
     * Convierte prioridad Todoist a formato local
     */
    private String convertirPrioridadDesdeTodoist(String prioridadTodoist) {
        if (prioridadTodoist == null) return "MEDIA";
        try {
            int priority = Integer.parseInt(prioridadTodoist);
            return switch (priority) {
                case 4 -> "ALTA";
                case 3 -> "ALTA";
                case 2 -> "MEDIA";
                case 1 -> "BAJA";
                default -> "MEDIA";
            };
        } catch (NumberFormatException e) {
            return "MEDIA";
        }
    }
    
    /**
     * Escapa caracteres especiales para JSON
     */
    private String escaparJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Modelo simple para tarea de Todoist
     */
    private static class TodoistTask {
        String content;
        String due;
        String priority;
    }
}
