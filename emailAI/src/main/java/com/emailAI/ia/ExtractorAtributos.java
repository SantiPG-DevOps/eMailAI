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

        atributos.add(new Attribute("longitud_asunto"));
        atributos.add(new Attribute("longitud_cuerpo"));
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
        atributos.add(new Attribute("longitud_cuerpo"));

        ArrayList<String> valoresClase = new ArrayList<>();
        valoresClase.add("NORMAL");
        valoresClase.add("URGENTE");
        atributos.add(new Attribute("prioridad", valoresClase));

        Instances data = new Instances("EmailPrioridad", atributos, 0);
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    /**
     * Convierte ejemplos de la BD a instancias Weka para entrenar SPAM.
     */
    public static Instances convertirAEstructura(List<DAOEntrenamiento.Ejemplo> ejemplos) {
        Instances data = construirEstructura();

        for (DAOEntrenamiento.Ejemplo e : ejemplos) {
            if (e == null || e.etiqueta == null) {
                continue;
            }

            String asunto = e.asunto != null ? e.asunto.toLowerCase() : "";
            String cuerpo = e.cuerpo != null ? e.cuerpo.toLowerCase() : "";

            double[] vals = new double[data.numAttributes()];
            vals[0] = asunto.length();
            vals[1] = cuerpo.length();
            vals[2] = (asunto.contains("oferta") || cuerpo.contains("oferta")) ? 1.0 : 0.0;
            vals[3] = (asunto.contains("gratis") || cuerpo.contains("gratis")) ? 1.0 : 0.0;
            vals[4] = (asunto.contains("%") || cuerpo.contains("%")) ? 1.0 : 0.0;

            String etiqueta = e.etiqueta.trim().toUpperCase();
            int idxClase = data.classAttribute().indexOfValue(etiqueta);

            if (idxClase != -1) {
                vals[5] = idxClase;
                DenseInstance inst = new DenseInstance(1.0, vals);
                inst.setDataset(data);
                data.add(inst);
            }
        }

        return data;
    }

    /**
     * Convierte ejemplos de la BD a instancias Weka para entrenar PRIORIDAD.
     */
    public static Instances convertirAEstructuraPrioridad(List<DAOEntrenamiento.Ejemplo> ejemplos) {
        Instances data = construirEstructuraPrioridad();

        for (DAOEntrenamiento.Ejemplo e : ejemplos) {
            if (e == null || e.etiqueta == null) {
                continue;
            }

            String asunto = e.asunto != null ? e.asunto.toLowerCase() : "";
            String cuerpo = e.cuerpo != null ? e.cuerpo.toLowerCase() : "";

            double[] vals = new double[data.numAttributes()];
            vals[0] = 0.0; // Placeholder de momento
            vals[1] = (cuerpo.contains("?") || cuerpo.contains("¿")) ? 1.0 : 0.0;
            vals[2] = asunto.startsWith("re:") ? 1.0 : 0.0;
            vals[3] = cuerpo.length();

            String etiqueta = e.etiqueta.trim().toUpperCase();
            int idxClase = data.classAttribute().indexOfValue(etiqueta);

            if (idxClase != -1) {
                vals[4] = idxClase;
                DenseInstance inst = new DenseInstance(1.0, vals);
                inst.setDataset(data);
                data.add(inst);
            }
        }

        return data;
    }
}