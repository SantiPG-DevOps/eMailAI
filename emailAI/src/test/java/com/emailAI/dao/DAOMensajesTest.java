package com.emailAI.dao;

import com.emailAI.model.Mensaje;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOMensajesTest {

    @Test
    void guardarModificarYActualizarCategoria_debeAplicarCambios() throws Exception {
        Path db = Files.createTempFile("emailai-mensajes-", ".db");
        DAOMensajes dao = new DAOMensajes("jdbc:sqlite:" + db);

        Mensaje m = new Mensaje("uid-1", "a@test.com", "Asunto", "Cuerpo");
        m.setUidImap("uid-1");
        m.setCarpetaImap("INBOX");
        m.setCategoria("DESCONOCIDO");
        m.setPrioridad("NORMAL");

        dao.guardarOModificar(List.of(m), "cuenta1", "INBOX");
        assertEquals(1, dao.listarPorCuentaHashYCarpeta("cuenta1", "INBOX").size());

        dao.actualizarCategoriaPrioridad("uid-1", "cuenta1", "INBOX", "SPAM", "NORMAL");
        Mensaje actualizado = dao.listarPorCuentaHashYCarpeta("cuenta1", "INBOX").get(0);
        assertEquals("SPAM", actualizado.getCategoria());
    }
}
