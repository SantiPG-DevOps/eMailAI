package com.emailAI.dao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DAOEventosCalendarioTest {

    @Test
    void guardarActualizarBorrarEvento_debeFuncionar() throws Exception {
        DAOEventosCalendario dao = new DAOEventosCalendario();
        LocalDate fecha = LocalDate.of(2026, 4, 1);

        dao.guardarEvento(fecha, "Demo", "detalle", "local");
        List<DAOEventosCalendario.Evento> eventos = dao.listarPorFecha(fecha);
        assertFalse(eventos.isEmpty());

        DAOEventosCalendario.Evento original = eventos.get(0);
        DAOEventosCalendario.Evento editado = new DAOEventosCalendario.Evento(
                original.id(),
                original.fecha(),
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
        assertTrue(dao.listarPorFecha(fecha).stream().noneMatch(e -> e.id() == original.id()));
    }

    @Test
    void fechasConEventosEnRango_debeDevolverDiasDistintos() throws Exception {
        DAOEventosCalendario dao = new DAOEventosCalendario();
        LocalDate d1 = LocalDate.of(2026, 5, 10);
        LocalDate d2 = LocalDate.of(2026, 5, 12);
        dao.guardarEvento(d1, "A", null, "local");
        dao.guardarEvento(d1, "B", null, "local");
        dao.guardarEvento(d2, "C", null, "local");

        Set<LocalDate> set = dao.fechasConEventosEnRango(d1, d2);
        assertEquals(2, set.size());
        assertTrue(set.contains(d1));
        assertTrue(set.contains(d2));

        for (DAOEventosCalendario.Evento e : dao.listarPorFecha(d1)) {
            dao.borrarEvento(e.id());
        }
        for (DAOEventosCalendario.Evento e : dao.listarPorFecha(d2)) {
            dao.borrarEvento(e.id());
        }
    }
}
