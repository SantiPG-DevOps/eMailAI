package com.emailAI.ia;

import com.emailAI.dao.DAOEntrenamiento;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

public class ExtractorAtributos {

    // --- ESTRUCTURAS DE DATOS (ATRIBUTOS) ---

    public static Instances construirEstructura() {
        ArrayList<Attribute> atributos = new ArrayList<>();
        atributos.add(new Attribute("long_asunto"));
        atributos.add(new Attribute("long_cuerpo"));
        atributos.add(new Attribute("contiene_oferta"));
        atributos.add(new Attribute("contiene_gratis"));
        atributos.add(new Attribute("contiene_porcentaje"));

        ArrayList<String> valoresClase = new ArrayList<>();
        valoresClase.add("SPAM");
        valoresClase.add("LEGITIMO");
        atributos.add(new Attribute("clase", valoresClase));

        Instances data = new Instances("EmailSpam", atributos, 0);
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    public static Instances construirEstructuraPrioridad() {
        ArrayList<Attribute> atributos = new ArrayList<>();
        atributos.add(new Attribute("es_remitente_frecuente")); 
        atributos.add(new Attribute("contiene_pregunta"));     
        atributos.add(new Attribute("es_respuesta_re"));       
        atributos.add(new Attribute("long_cuerpo"));           

        ArrayList<String> valoresClase = new ArrayList<>();
        valoresClase.add("NORMAL");
        valoresClase.add("URGENTE");
        atributos.add(new Attribute("prioridad", valoresClase));

        Instances data = new Instances("EmailPrioridad", atributos, 0);
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    // --- CONVERSORES DE LISTAS (ENTRENAMIENTO) ---

    /**
     * Convierte ejemplos de la DB a formato Weka para detectar SPAM
     */
    public static Instances convertirAEstructura(List<DAOEntrenamiento.Ejemplo> ejemplos) {
        Instances data = construirEstructura();

        for (DAOEntrenamiento.Ejemplo e : ejemplos) {
            double[] vals = new double[data.numAttributes()];
            String asunto = e.asunto != null ? e.asunto.toLowerCase() : "";
            String cuerpo = e.cuerpo != null ? e.cuerpo.toLowerCase() : "";

            vals[0] = asunto.length();
            vals[1] = cuerpo.length();
            vals[2] = (asunto.contains("oferta") || cuerpo.contains("oferta")) ? 1.0 : 0.0;
            vals[3] = (asunto.contains("gratis") || cuerpo.contains("gratis")) ? 1.0 : 0.0;
            vals[4] = (asunto.contains("%") || cuerpo.contains("%")) ? 1.0 : 0.0;
            
            // Mapeo de etiqueta: SPAM o LEGITIMO
            vals[5] = data.classAttribute().indexOfValue(e.etiqueta);

            if (vals[5] != -1) data.add(new DenseInstance(1.0, vals));
        }
        return data;
    }

    /**
     * Convierte ejemplos de la DB a formato Weka para detectar PRIORIDAD
     */
    public static Instances convertirAEstructuraPrioridad(List<DAOEntrenamiento.Ejemplo> ejemplos) {
        Instances data = construirEstructuraPrioridad();

        for (DAOEntrenamiento.Ejemplo e : ejemplos) {
            double[] vals = new double[data.numAttributes()];
            String asunto = e.asunto != null ? e.asunto.toLowerCase() : "";
            String cuerpo = e.cuerpo != null ? e.cuerpo.toLowerCase() : "";

            // Lógica de extracción para prioridad
            vals[0] = 0.0; // Placeholder para remitente frecuente
            vals[1] = (cuerpo.contains("?") || cuerpo.contains("¿")) ? 1.0 : 0.0;
            vals[2] = (asunto.startsWith("re:") || asunto.startsWith("fwd:")) ? 1.0 : 0.0;
            vals[3] = cuerpo.length();
            
            // Mapeo de etiqueta: NORMAL o URGENTE
            // Nota: Asegúrate de que al guardar el ejemplo en la DB, la etiqueta coincida
            vals[4] = data.classAttribute().indexOfValue(e.etiqueta);

            if (vals[4] != -1) data.add(new DenseInstance(1.0, vals));
        }
        return data;
    }
}