package com.emailAI.dao;

import com.emailAI.model.Contacto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOContactosTest {

    private DAOContactos dao;

    @BeforeEach
    void setUp() {
        dao = new DAOContactos();

        // Limpiar contactos existentes para aislar tests
        for (Contacto c : dao.listarTodos()) {
            dao.borrar(c);
        }
    }

    @Test
    void guardarYListar_debePersistirYDescifrarDatos() {
        Contacto c = new Contacto(
                null,
                "Ana",
                null,
                "ana@test.com",
                "123456",
                "proveedor"
        );

        dao.guardarOActualizar(c);

        assertNotNull(c.getId());

        List<Contacto> lista = dao.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).getNombre());
        assertEquals("ana@test.com", lista.get(0).getEmail());
        assertEquals("123456", lista.get(0).getTelefono());
        assertEquals("proveedor", lista.get(0).getNotas());
    }

    @Test
    void borrar_debeEliminarContacto() {
        Contacto c = new Contacto(
                null,
                "Luis",
                null,
                "luis@test.com",
                "",
                ""
        );

        dao.guardarOActualizar(c);
        dao.borrar(c);

        assertTrue(dao.listarTodos().isEmpty());
    }
}