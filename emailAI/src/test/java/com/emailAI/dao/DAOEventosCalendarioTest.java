package com.emailAI.dao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

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
}
