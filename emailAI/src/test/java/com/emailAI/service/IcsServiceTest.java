package com.emailAI.service;

import com.emailAI.dao.DAOEventosCalendario.Evento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para IcsService.
 * Verifica la correcta importación y exportación de archivos ICS.
 */
public class IcsServiceTest {

    @TempDir
    Path tempDir;
    
    private IcsService icsService;
    private Evento eventoPrueba;
    
    @BeforeEach
    void setUp() throws Exception {
        icsService = new IcsService();
        
        eventoPrueba = new Evento(
            1,
            LocalDate.of(2026, 4, 2),
            LocalTime.of(10, 30),
            "Reunión importante",
            "Reunión de seguimiento del proyecto emailAI",
            "test"
        );
    }
    
    @Test
    @DisplayName("Exportar calendario a archivo ICS")
    void testExportarCalendarioAICS() throws Exception {
        // Guardar evento de prueba en la base de datos
        icsService.getDaoEventos().guardarEvento(
            eventoPrueba.fecha(),
            eventoPrueba.hora(),
            eventoPrueba.titulo(),
            eventoPrueba.detalle(),
            eventoPrueba.origen()
        );
        
        // Exportar a archivo temporal
        Path archivoIcs = tempDir.resolve("test_export.ics");
        icsService.exportarCalendarioAICS(archivoIcs.toString());
        
        // Verificar que el archivo existe
        assertTrue(Files.exists(archivoIcs), "El archivo ICS debe existir");
        
        // Verificar contenido básico del archivo
        String contenido = Files.readString(archivoIcs);
        assertTrue(contenido.contains("BEGIN:VCALENDAR"), "Debe contener inicio de calendario");
        assertTrue(contenido.contains("END:VCALENDAR"), "Debe contener fin de calendario");
        assertTrue(contenido.contains("BEGIN:VEVENT"), "Debe contener inicio de evento");
        assertTrue(contenido.contains("END:VEVENT"), "Debe contener fin de evento");
        assertTrue(contenido.contains("SUMMARY:" + eventoPrueba.titulo()), "Debe contener título del evento");
        assertTrue(contenido.contains("UID:" + eventoPrueba.id() + "@emailai"), "Debe contener UID del evento");
    }
    
    @Test
    @DisplayName("Exportar calendario vacío debe lanzar excepción")
    void testExportarCalendarioVacio() {
        Path archivoIcs = tempDir.resolve("test_vacio.ics");
        
        assertThrows(IOException.class, () -> {
            icsService.exportarCalendarioAICS(archivoIcs.toString());
        }, "Debe lanzar IOException cuando no hay eventos");
    }
    
    @Test
    @DisplayName("Importar archivo ICS válido")
    void testImportarArchivoICSValido() throws Exception {
        // Crear archivo ICS de prueba
        String contenidoICS = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Test//Calendar//EN
                BEGIN:VEVENT
                UID:test-event-1@test.com
                DTSTART:20260402T103000Z
                DTEND:20260402T113000Z
                SUMMARY:Evento de prueba
                DESCRIPTION:Descripción del evento de prueba
                END:VEVENT
                END:VCALENDAR
                """;
        
        Path archivoIcs = tempDir.resolve("test_import.ics");
        Files.writeString(archivoIcs, contenidoICS);
        
        // Importar archivo
        List<Evento> eventos = icsService.procesarArchivoIcs(archivoIcs.toString());
        
        // Verificar que se importó correctamente
        assertEquals(1, eventos.size(), "Debe importar un evento");
        
        Evento eventoImportado = eventos.get(0);
        assertEquals("Evento de prueba", eventoImportado.titulo(), "El título debe coincidir");
        assertEquals(LocalDate.of(2026, 4, 2), eventoImportado.fecha(), "La fecha debe coincidir");
        assertEquals(LocalTime.of(10, 30), eventoImportado.hora(), "La hora debe coincidir");
        assertEquals("Descripción del evento de prueba", eventoImportado.detalle(), "La descripción debe coincidir");
        assertEquals("ics", eventoImportado.origen(), "El origen debe ser 'ics'");
    }
    
    @Test
    @DisplayName("Importar archivo ICS inexistente debe lanzar excepción")
    void testImportarArchivoICSInexistente() {
        Path archivoInexistente = tempDir.resolve("no_existe.ics");
        
        assertThrows(IOException.class, () -> {
            icsService.procesarArchivoIcs(archivoInexistente.toString());
        }, "Debe lanzar IOException cuando el archivo no existe");
    }
    
    @Test
    @DisplayName("Importar archivo ICS con formato inválido")
    void testImportarArchivoICSFormatoInvalido() throws Exception {
        // Crear archivo con formato inválido
        String contenidoInvalido = """
                ESTO NO ES UN ARCHIVO ICS
                SOLO TEXTO ALEATORIO
                """;
        
        Path archivoInvalido = tempDir.resolve("invalido.ics");
        Files.writeString(archivoInvalido, contenidoInvalido);
        
        // Importar archivo inválido
        List<Evento> eventos = icsService.procesarArchivoIcs(archivoInvalido.toString());
        
        // Debe devolver lista vacía por no poder parsear eventos válidos
        assertTrue(eventos.isEmpty(), "No debe importar eventos de archivo inválido");
    }
    
    @Test
    @DisplayName("Verificar cambios en archivo ICS")
    void testHaCambiadoDesdeUltimaSincronizacion() throws Exception {
        // Crear archivo ICS
        Path archivoIcs = tempDir.resolve("test_sync.ics");
        Files.writeString(archivoIcs, "BEGIN:VCALENDAR\nEND:VCALENDAR");
        
        // Primera verificación (no hay sincronización previa)
        assertTrue(icsService.haCambiadoDesdeUltimaSincronizacion(archivoIcs.toString()),
                  "Debe detectar cambios en archivo nuevo");
        
        // Simular que pasa tiempo y se crea archivo de sincronización
        Thread.sleep(100);
        Files.writeString(tempDir.resolve("test_sync.ics.sync"), "123456789");
        
        // Segunda verificación (mismo archivo, mismo timestamp)
        assertFalse(icsService.haCambiadoDesdeUltimaSincronizacion(archivoIcs.toString()),
                   "No debe detectar cambios si el archivo no fue modificado");
        
        // Modificar archivo
        Thread.sleep(100);
        Files.writeString(archivoIcs, "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR");
        
        // Tercera verificación (archivo modificado)
        assertTrue(icsService.haCambiadoDesdeUltimaSincronizacion(archivoIcs.toString()),
                  "Debe detectar cambios cuando el archivo fue modificado");
    }
    
    @Test
    @DisplayName("Sincronizar solo si es necesario")
    void testSincronizarSiEsNecesario() throws Exception {
        // Crear archivo ICS
        Path archivoIcs = tempDir.resolve("test_sync_condicional.ics");
        String contenidoICS = """
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:sync-test-1@test.com
                DTSTART:20260402T140000Z
                DTEND:20260402T150000Z
                SUMMARY:Evento para sincronización
                END:VEVENT
                END:VCALENDAR
                """;
        Files.writeString(archivoIcs, contenidoICS);
        
        // Primera sincronización (debe procesar)
        List<Evento> eventos1 = icsService.sincronizarSiEsNecesario(archivoIcs.toString());
        assertEquals(1, eventos1.size(), "Debe sincronizar en la primera ejecución");
        
        // Segunda sincronización (no debe procesar, archivo no modificado)
        List<Evento> eventos2 = icsService.sincronizarSiEsNecesario(archivoIcs.toString());
        assertTrue(eventos2.isEmpty(), "No debe sincronizar si el archivo no cambió");
        
        // Modificar archivo y sincronizar nuevamente
        Thread.sleep(100);
        Files.writeString(archivoIcs, contenidoICS + "\nMODIFICADO");
        List<Evento> eventos3 = icsService.sincronizarSiEsNecesario(archivoIcs.toString());
        assertEquals(1, eventos3.size(), "Debe sincronizar después de modificar el archivo");
    }
    
    @Test
    @DisplayName("Formatear fecha ICS correctamente")
    void testFormatearFechaICS() throws Exception {
        // Usar reflexión para acceder al método privado
        var method = IcsService.class.getDeclaredMethod("formatearFechaICS", 
                LocalDate.class, LocalTime.class);
        method.setAccessible(true);
        
        LocalDate fecha = LocalDate.of(2026, 4, 2);
        LocalTime hora = LocalTime.of(14, 30, 15);
        
        String resultado = (String) method.invoke(icsService, fecha, hora);
        
        assertEquals("20260402T143015Z", resultado, "Debe formatear fecha correctamente");
    }
    
    @Test
    @DisplayName("Escapar texto ICS correctamente")
    void testEscapeTextoICS() throws Exception {
        // Usar reflexión para acceder al método privado
        var method = IcsService.class.getDeclaredMethod("escapeTextoICS", String.class);
        method.setAccessible(true);
        
        String textoOriginal = "Texto con, caracteres; especiales\\ y\nsaltos";
        String resultado = (String) method.invoke(icsService, textoOriginal);
        
        assertEquals("Texto con\\, caracteres\\; especiales\\\\ y\\nsaltos", resultado,
                     "Debe escapar caracteres especiales correctamente");
    }
}
