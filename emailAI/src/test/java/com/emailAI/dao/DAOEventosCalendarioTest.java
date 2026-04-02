package com.emailAI.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DAOEventosCalendarioTest {

    private DAOEventosCalendario dao;

    @BeforeEach
    void setUp() throws Exception {
        dao = new DAOEventosCalendario();

        LocalDate desde = LocalDate.of(2000, 1, 1);
        LocalDate hasta = LocalDate.of(2100, 12, 31);

        for (LocalDate fecha : dao.fechasConEventosEnRango(desde, hasta)) {
            for (DAOEventosCalendario.Evento e : dao.listarPorFecha(fecha)) {
                dao.borrarEvento(e.id());
            }
        }
    }

    @Test
    void guardarActualizarBorrarEvento_debeFuncionar() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 4, 1);
        LocalTime hora = LocalTime.of(10, 30);

        dao.guardarEvento(fecha, hora, "Demo", "detalle", "local");

        List<DAOEventosCalendario.Evento> eventos = dao.listarPorFecha(fecha);
        assertFalse(eventos.isEmpty());

        DAOEventosCalendario.Evento original = eventos.get(0);

        DAOEventosCalendario.Evento editado = new DAOEventosCalendario.Evento(
                original.id(),
                original.fecha(),
                original.hora(),
                "Demo editada",
                original.detalle(),
                original.origen()
        );

        dao.actualizarEvento(editado);

        DAOEventosCalendario.Evento actualizado = dao.listarPorFecha(fecha).stream()
                .filter(e -> e.id() == original.id())
                .findFirst()
                .orElseThrow();

        assertEquals("Demo editada", actualizado.titulo());

        dao.borrarEvento(original.id());

        assertTrue(dao.listarPorFecha(fecha).stream()
                .noneMatch(e -> e.id() == original.id()));
    }

    @Test
    void fechasConEventosEnRango_debeDevolverDiasDistintos() throws Exception {
        LocalDate d1 = LocalDate.of(2026, 5, 10);
        LocalDate d2 = LocalDate.of(2026, 5, 12);
        LocalTime hora = LocalTime.of(9, 0);

        dao.guardarEvento(d1, hora, "A", null, "local");
        dao.guardarEvento(d1, hora, "B", null, "local");
        dao.guardarEvento(d2, hora, "C", null, "local");

        Set<LocalDate> set = dao.fechasConEventosEnRango(d1, d2);

        assertEquals(2, set.size());
        assertTrue(set.contains(d1));
        assertTrue(set.contains(d2));
    }
}