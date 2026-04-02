package com.emailAI.dao;

import com.emailAI.model.Mensaje;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOMensajesTest {

    @Test
    void guardarModificarYActualizarCategoria_debeAplicarCambios() throws Exception {
        DAOMensajes dao = new DAOMensajes();

        String cuenta = "cuenta-test-mensajes";
        String carpeta = "INBOX";
        String uid = "uid-test-mensajes-001";

        Mensaje m = new Mensaje(uid, "a@test.com", "Asunto", "Cuerpo");
        m.setUidImap(uid);
        m.setCarpetaImap(carpeta);
        m.setCategoria("DESCONOCIDO");
        m.setPrioridad("NORMAL");

        dao.guardarOModificar(List.of(m), cuenta, carpeta);

        List<Mensaje> mensajes = dao.listarPorCuentaHashYCarpeta(cuenta, carpeta);
        assertFalse(mensajes.isEmpty());

        Mensaje guardado = mensajes.stream()
                .filter(x -> uid.equals(x.getUidImap()))
                .findFirst()
                .orElseThrow();

        assertEquals("DESCONOCIDO", guardado.getCategoria());

        dao.actualizarCategoriaPrioridad(uid, cuenta, carpeta, "SPAM", "NORMAL");

        Mensaje actualizado = dao.listarPorCuentaHashYCarpeta(cuenta, carpeta).stream()
                .filter(x -> uid.equals(x.getUidImap()))
                .findFirst()
                .orElseThrow();

        assertEquals("SPAM", actualizado.getCategoria());
    }
}