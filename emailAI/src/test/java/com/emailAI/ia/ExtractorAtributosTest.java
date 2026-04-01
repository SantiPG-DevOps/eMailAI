package com.emailAI.ia;

import com.emailAI.dao.DAOEntrenamiento;
import org.junit.jupiter.api.Test;
import weka.core.Instances;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtractorAtributosTest {

    @Test
    void construirEstructura_debeDefinirClaseSpamLegitimo() {
        Instances data = ExtractorAtributos.construirEstructura();

        assertEquals(6, data.numAttributes());
        assertEquals(5, data.classIndex());
        assertEquals("SPAM", data.classAttribute().value(0));
        assertEquals("LEGITIMO", data.classAttribute().value(1));
    }

    @Test
    void convertirAEstructura_debeAgregarInstanciasValidas() {
        List<DAOEntrenamiento.Ejemplo> ejemplos = List.of(
                new DAOEntrenamiento.Ejemplo("a@a.com", "Oferta gratis", "50% descuento", "SPAM", "SPAM"),
                new DAOEntrenamiento.Ejemplo("b@b.com", "Reunión", "Mañana a las 10", "LEGITIMO", "SPAM")
        );

        Instances data = ExtractorAtributos.convertirAEstructura(ejemplos);
        assertEquals(2, data.numInstances());
        assertEquals("SPAM", data.classAttribute().value((int) data.instance(0).classValue()));
        assertEquals("LEGITIMO", data.classAttribute().value((int) data.instance(1).classValue()));
    }
}
