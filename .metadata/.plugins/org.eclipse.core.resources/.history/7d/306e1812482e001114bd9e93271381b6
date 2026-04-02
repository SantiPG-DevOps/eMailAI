package com.emailAI.ia;

import com.emailAI.dao.DAOEntrenamiento;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

// Define estructuras Weka y transforma ejemplos de BD a vectores de entrenamiento.
public class ExtractorAtributos {

    // Construye el esquema de atributos para el modelo SPAM/LEGITIMO.
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

    // Construye el esquema de atributos para el modelo NORMAL/URGENTE.
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

    /**
     * Convierte ejemplos de la DB a formato Weka para detectar SPAM
     */
    // Convierte ejemplos etiquetados a instancias Weka para entrenar spam.
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
            
            // Mapea la etiqueta textual a su índice de clase para spam.
            vals[5] = data.classAttribute().indexOfValue(e.etiqueta);

            if (vals[5] != -1) data.add(new DenseInstance(1.0, vals));
        }
        return data;
    }

    /**
     * Convierte ejemplos de la DB a formato Weka para detectar PRIORIDAD
     */
    // Convierte ejemplos etiquetados a instancias Weka para entrenar prioridad.
    public static Instances convertirAEstructuraPrioridad(List<DAOEntrenamiento.Ejemplo> ejemplos) {
        Instances data = construirEstructuraPrioridad();

        for (DAOEntrenamiento.Ejemplo e : ejemplos) {
            double[] vals = new double[data.numAttributes()];
            String asunto = e.asunto != null ? e.asunto.toLowerCase() : "";
            String cuerpo = e.cuerpo != null ? e.cuerpo.toLowerCase() : "";

            // Extrae señales simples para el modelo de prioridad.
            vals[0] = 0.0; // Placeholder para remitente frecuente
            vals[1] = (cuerpo.contains("?") || cuerpo.contains("¿")) ? 1.0 : 0.0;
            vals[2] = (asunto.startsWith("re:") || asunto.startsWith("fwd:")) ? 1.0 : 0.0;
            vals[3] = cuerpo.length();
            
            // Mapea la etiqueta textual a su índice de clase para prioridad.
            vals[4] = data.classAttribute().indexOfValue(e.etiqueta);

            if (vals[4] != -1) data.add(new DenseInstance(1.0, vals));
        }
        return data;
    }
}