package com.emailAI.ia;

import com.emailAI.model.Mensaje;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Orquesta entrenamiento, carga en caché y predicción de modelos clásicos Weka.
public class GestorModelos {

    private static final String RUTA_MODELO_SPAM      = "modelo-spam.model"; // Fichero del modelo de clasificación spam.
    private static final String RUTA_MODELO_PRIORIDAD = "prioridad.model"; // Fichero del modelo de prioridad.

    // Mantiene en memoria modelos ya cargados para evitar lecturas repetidas.
    private static final Map<String, Classifier> modelosCargados = new HashMap<>();

    /**
     * Entrena y guarda un modelo según el tipo: "SPAM" o "PRIORIDAD".
     * Usa NaiveBayes por defecto.
     */
    // Entrena un NaiveBayes con los datos dados y guarda el modelo según su tipo.
    public static void entrenarYGuardar(Instances data, String tipo) throws Exception {
        if (data == null || data.numInstances() < 5) {
            throw new IllegalArgumentException("Faltan datos para entrenar el modelo");
        }

        Classifier cls = new NaiveBayes();
        cls.buildClassifier(data);

        String ruta;
        if ("SPAM".equalsIgnoreCase(tipo)) {
            ruta = RUTA_MODELO_SPAM;
        } else if ("PRIORIDAD".equalsIgnoreCase(tipo)) {
            ruta = RUTA_MODELO_PRIORIDAD;
        } else {
            throw new IllegalArgumentException("Tipo de modelo desconocido: " + tipo);
        }

        SerializationHelper.write(ruta, cls);
        modelosCargados.put(ruta, cls);
    }

    /**
     * Clasifica un mensaje como SPAM/LEGITIMO/DESCONOCIDO usando el modelo de SPAM.
     */
    // Clasifica un mensaje con el modelo de spam o devuelve DESCONOCIDO si no existe.
    public static String clasificarSpam(Mensaje mensaje) throws Exception {
        if (!Files.exists(Path.of(RUTA_MODELO_SPAM))) {
            return "DESCONOCIDO";
        }
        Classifier cls = obtenerModelo(RUTA_MODELO_SPAM);
        Instances estructura = ExtractorAtributos.construirEstructura();
        double[] vals = extraerValoresSpam(mensaje, estructura);
        return predecir(cls, estructura, vals);
    }

    /**
     * Clasifica la prioridad de un mensaje (por ejemplo NORMAL/URGENTE).
     */
    // Clasifica la prioridad del mensaje o devuelve NORMAL sin modelo entrenado.
    public static String clasificarPrioridad(Mensaje mensaje) throws Exception {
        if (!Files.exists(Path.of(RUTA_MODELO_PRIORIDAD))) {
            return "NORMAL";
        }
        Classifier cls = obtenerModelo(RUTA_MODELO_PRIORIDAD);
        Instances estructura = ExtractorAtributos.construirEstructuraPrioridad();
        double[] vals = extraerValoresPrioridad(mensaje, estructura);
        return predecir(cls, estructura, vals);
    }

    // Recupera un modelo desde caché o disco y lo deja disponible en memoria.
    private static Classifier obtenerModelo(String ruta) throws Exception {
        if (!modelosCargados.containsKey(ruta)) {
            modelosCargados.put(ruta, (Classifier) SerializationHelper.read(ruta));
        }
        return modelosCargados.get(ruta);
    }

    // Ejecuta la inferencia sobre una instancia y devuelve la etiqueta de clase resultante.
    private static String predecir(Classifier cls, Instances estructura, double[] vals) throws Exception {
        DenseInstance inst = new DenseInstance(1.0, vals);
        inst.setDataset(estructura);
        double idxClase = cls.classifyInstance(inst);
        return estructura.classAttribute().value((int) idxClase);
    }

    /**
     * Extrae atributos para el modelo de SPAM.
     * IMPORTANTE: el tamaño del array debe coincidir con el número de atributos definididos
     * en ExtractorAtributos.construirEstructura(), excluyendo la clase.
     */
    // Extrae atributos numéricos heurísticos para alimentar el modelo de spam.
    private static double[] extraerValoresSpam(Mensaje mensaje, Instances estructura) {
        String as = (mensaje.getAsunto() != null ? mensaje.getAsunto() : "").toLowerCase();
        String cu = (mensaje.getCuerpo() != null ? mensaje.getCuerpo() : "").toLowerCase();

        // Asume cinco atributos numéricos previos a la clase en la estructura de spam.
        return new double[] {
                as.length(),                                                // longitud asunto
                cu.length(),                                                // longitud cuerpo
                (as.contains("oferta") || cu.contains("oferta")) ? 1.0 : 0.0,
                (as.contains("gratis") || cu.contains("gratis")) ? 1.0 : 0.0,
                (as.contains("%") || cu.contains("%")) ? 1.0 : 0.0
        };
    }

    /**
     * Extrae atributos para el modelo de PRIORIDAD.
     * Igual: el tamaño del array debe cuadrar con construirEstructuraPrioridad().
     */
    // Extrae atributos simples para alimentar el modelo de prioridad.
    private static double[] extraerValoresPrioridad(Mensaje mensaje, Instances estructura) {
        String as = (mensaje.getAsunto() != null ? mensaje.getAsunto() : "").toLowerCase();
        String cu = (mensaje.getCuerpo() != null ? mensaje.getCuerpo() : "").toLowerCase();

        return new double[] {
                cu.contains("?") ? 1.0 : 0.0,      // hay pregunta en el cuerpo
                as.startsWith("re:") ? 1.0 : 0.0,  // es respuesta
                cu.length()                        // longitud del cuerpo
        };
    }
}
