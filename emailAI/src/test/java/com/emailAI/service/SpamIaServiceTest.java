package com.emailAI.service;

import com.emailAI.model.Mensaje;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpamIaServiceTest {

    @Test
    void entrenarYClasificar_debeRetornarClaseValida() throws Exception {
        Path modelosDir = Files.createTempDirectory("emailai-modelos-");
        SpamIaService service = new SpamIaService(modelosDir);

        Mensaje legit = new Mensaje("1", "a@test.com", "Reunión equipo", "Vemos pendientes del sprint");
        legit.setCategoria("LEGITIMO");
        Mensaje spam = new Mensaje("2", "promo@test.com", "Oferta gratis", "50% de descuento gratis");
        spam.setCategoria("SPAM");

        service.entrenarModelo("cuenta-test", List.of(legit, spam));
        SpamIaService.ClaseCorreo clase = service.clasificar("cuenta-test",
                new Mensaje("3", "x@test.com", "Consulta", "Necesito ayuda con el proyecto"));

        assertNotNull(clase);
    }
}
