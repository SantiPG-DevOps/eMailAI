package com.emailAI.dao;

import com.emailAI.model.Tarea;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOTareasTest {

    @Test
    void guardarActualizarBorrar_debeMantenerConsistencia() throws Exception {
        Path db = Files.createTempFile("emailai-tareas-", ".db");
        DAOTareas dao = new DAOTareas("jdbc:sqlite:" + db);

        Tarea tarea = new Tarea(null, "Preparar release", "Checklist final", LocalDate.now(), "PENDIENTE", "release");
        dao.guardarOActualizar(tarea);
        assertNotNull(tarea.getId());

        List<Tarea> todas = dao.listarTodas();
        assertEquals(1, todas.size());
        assertEquals("Preparar release", todas.get(0).getTitulo());

        tarea.setEstado("COMPLETADA");
        dao.guardarOActualizar(tarea);
        assertEquals("COMPLETADA", dao.listarTodas().get(0).getEstado());

        dao.borrar(tarea);
        assertTrue(dao.listarTodas().isEmpty());
    }
}
