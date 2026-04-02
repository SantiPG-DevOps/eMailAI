package com.emailAI.dao;

import com.emailAI.model.Tarea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOTareasTest {

    private DAOTareas dao;

    @BeforeEach
    void setUp() throws Exception {
        dao = new DAOTareas();

        // Limpiar tareas previas para no arrastrar datos entre tests
        for (Tarea t : dao.listarTodas()) {
            dao.borrar(t);
        }
    }

    @Test
    void guardarActualizarBorrar_debeMantenerConsistencia() throws Exception {
        Tarea tarea = new Tarea(
                null,
                "Preparar release",
                "Checklist final",
                LocalDate.now(),
                "PENDIENTE",
                "release"
        );

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