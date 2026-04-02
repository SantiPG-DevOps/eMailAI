package com.emailAI.service;

import com.emailAI.controller.ConfigController;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Servicio de sincronización periódica para calendario y Todoist.
 * Se ejecuta cada 30 minutos si hubo novedades.
 */
public class SyncSchedulerService {
    
    private static final String PREF_LAST_SYNC = "last_sync_timestamp";
    private static final int SYNC_INTERVAL_MINUTES = 30;
    
    private final IcsService icsService;
    private TodoistService todoistService;
    private ScheduledExecutorService scheduler;
    private long lastSyncTimestamp;
    
    public SyncSchedulerService() throws Exception {
        this.icsService = new IcsService();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "emailai-sync-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.lastSyncTimestamp = obtenerUltimaSincronizacion();
    }
    
    /**
     * Inicia el servicio de sincronización periódica
     */
    public void iniciarSincronizacion() {
        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
        
        // Verificar si la sincronización está habilitada
        boolean icsEnabled = prefs.getBoolean(ConfigController.PREF_ICS_ENABLED, false);
        boolean todoistEnabled = prefs.getBoolean(ConfigController.PREF_TODOIST_ENABLED, false);
        
        if (!icsEnabled && !todoistEnabled) {
            System.out.println("Sincronización deshabilitada en configuración");
            return;
        }
        
        // Programar sincronización inicial inmediata
        scheduler.schedule(this::realizarSincronizacionProgramada, 0, TimeUnit.SECONDS);
        
        // Programar sincronización periódica cada 30 minutos
        scheduler.scheduleAtFixedRate(
            this::realizarSincronizacionProgramada,
            SYNC_INTERVAL_MINUTES,
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        System.out.println("Servicio de sincronización iniciado - intervalo: " + SYNC_INTERVAL_MINUTES + " minutos");
    }
    
    /**
     * Detiene el servicio de sincronización
     */
    public void detenerSincronizacion() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servicio de sincronización detenido");
        }
    }
    
    /**
     * Realiza la sincronización programada si hay novedades
     */
    private void realizarSincronizacionProgramada() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
            boolean icsEnabled = prefs.getBoolean(ConfigController.PREF_ICS_ENABLED, false);
            boolean todoistEnabled = prefs.getBoolean(ConfigController.PREF_TODOIST_ENABLED, false);
            
            boolean huboNovedades = false;
            
            // Sincronizar ICS si está habilitado
            if (icsEnabled) {
                String icsPath = prefs.get(ConfigController.PREF_ICS_PATH, "");
                if (!icsPath.isBlank()) {
                    List<com.emailAI.dao.DAOEventosCalendario.Evento> eventos = 
                        icsService.sincronizarSiEsNecesario(icsPath);
                    if (!eventos.isEmpty()) {
                        huboNovedades = true;
                        System.out.println("Sincronizados " + eventos.size() + " eventos desde ICS");
                    }
                }
            }
            
            // Sincronizar Todoist si está habilitado
            if (todoistEnabled) {
                String apiKey = prefs.get(ConfigController.PREF_TODOIST_API_KEY, "");
                if (!apiKey.isBlank()) {
                    if (todoistService == null) {
                        todoistService = new TodoistService(apiKey);
                    }
                    
                    List<String> errores = todoistService.sincronizarTareasConTodoist();
                    if (errores.isEmpty()) {
                        huboNovedades = true;
                        System.out.println("Sincronización con Todoist completada sin errores");
                    } else {
                        System.err.println("Errores en sincronización con Todoist:");
                        errores.forEach(System.err::println);
                    }
                }
            }
            
            // Actualizar timestamp de última sincronización si hubo novedades
            if (huboNovedades) {
                lastSyncTimestamp = System.currentTimeMillis();
                guardarUltimaSincronizacion(lastSyncTimestamp);
            }
            
        } catch (Exception e) {
            System.err.println("Error en sincronización programada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fuerza una sincronización inmediata
     */
    public void sincronizarAhora() {
        scheduler.schedule(this::realizarSincronizacionProgramada, 0, TimeUnit.SECONDS);
    }
    
    /**
     * Verifica si hay cambios pendientes de sincronizar
     */
    public boolean hayCambiosPendientes() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
            boolean icsEnabled = prefs.getBoolean(ConfigController.PREF_ICS_ENABLED, false);
            
            if (icsEnabled) {
                String icsPath = prefs.get(ConfigController.PREF_ICS_PATH, "");
                if (!icsPath.isBlank() && icsService.haCambiadoDesdeUltimaSincronizacion(icsPath)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error verificando cambios pendientes: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Obtiene el timestamp de la última sincronización
     */
    private long obtenerUltimaSincronizacion() {
        Preferences prefs = Preferences.userNodeForPackage(SyncSchedulerService.class);
        return prefs.getLong(PREF_LAST_SYNC, 0);
    }
    
    /**
     * Guarda el timestamp de la última sincronización
     */
    private void guardarUltimaSincronizacion(long timestamp) {
        Preferences prefs = Preferences.userNodeForPackage(SyncSchedulerService.class);
        prefs.putLong(PREF_LAST_SYNC, timestamp);
    }
    
    /**
     * Obtiene el estado actual del servicio
     */
    public boolean estaActivo() {
        return scheduler != null && !scheduler.isShutdown();
    }
    
    /**
     * Obtiene información del estado actual para depuración
     */
    public String obtenerEstadoInfo() {
        if (!estaActivo()) {
            return "Servicio detenido";
        }
        
        long minutosDesdeUltimaSync = (System.currentTimeMillis() - lastSyncTimestamp) / (60 * 1000);
        return String.format("Activo - Última sincronización: hace %d minutos", minutosDesdeUltimaSync);
    }
}
