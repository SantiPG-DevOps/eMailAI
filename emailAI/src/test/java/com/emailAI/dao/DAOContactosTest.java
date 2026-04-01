package com.emailAI.dao;

import com.emailAI.model.Contacto;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOContactosTest {

    @Test
    void guardarYListar_debePersistirYDescifrarDatos() throws Exception {
        Path db = Files.createTempFile("emailai-contactos-", ".db");
        DAOContactos dao = new DAOContactos("jdbc:sqlite:" + db);

        Contacto c = new Contacto(null, "Ana", "ana@test.com", "123456", "proveedor");
        dao.guardarOActualizar(c);
        assertNotNull(c.getId());

        List<Contacto> lista = dao.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).getNombre());
        assertEquals("ana@test.com", lista.get(0).getEmail());
        assertEquals("123456", lista.get(0).getTelefono());
    }

    @Test
    void borrar_debeEliminarContacto() throws Exception {
        Path db = Files.createTempFile("emailai-contactos-del-", ".db");
        DAOContactos dao = new DAOContactos("jdbc:sqlite:" + db);

        Contacto c = new Contacto(null, "Luis", "luis@test.com", "", "");
        dao.guardarOActualizar(c);
        dao.borrar(c);

        assertTrue(dao.listarTodos().isEmpty());
    }
}
