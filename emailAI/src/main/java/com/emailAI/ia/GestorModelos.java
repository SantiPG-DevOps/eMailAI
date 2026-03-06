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

public class GestorModelos {
    private static final String RUTA_MODELO_SPAM = "modelo-spam.model";
    private static final String RUTA_MODELO_PRIORIDAD = "prioridad.model";
    private static final Map<String, Classifier> modelosCargados = new HashMap<>();

    // Fíjate en los DOS parámetros: Instances y String
    public static void entrenarYGuardar(Instances data, String tipo) throws Exception {
        if (data.numInstances() < 5) throw new IllegalArgumentException("Faltan datos");
        Classifier cls = new NaiveBayes();
        cls.buildClassifier(data);
        String ruta = tipo.equalsIgnoreCase("SPAM") ? RUTA_MODELO_SPAM : RUTA_MODELO_PRIORIDAD;
        SerializationHelper.write(ruta, cls);
        modelosCargados.put(ruta, cls);
    }

    public static String clasificarSpam(Mensaje mensaje) throws Exception {
        if (!Files.exists(Path.of(RUTA_MODELO_SPAM))) return "DESCONOCIDO";
        Classifier cls = obtenerModelo(RUTA_MODELO_SPAM);
        Instances estructura = ExtractorAtributos.construirEstructura();
        return predecir(cls, estructura, extraerValoresSpam(mensaje, estructura));
    }

    public static String clasificarPrioridad(Mensaje mensaje) throws Exception {
        if (!Files.exists(Path.of(RUTA_MODELO_PRIORIDAD))) return "NORMAL";
        Classifier cls = obtenerModelo(RUTA_MODELO_PRIORIDAD);
        Instances estructura = ExtractorAtributos.construirEstructuraPrioridad();
        return predecir(cls, estructura, extraerValoresPrioridad(mensaje, estructura));
    }

    private static Classifier obtenerModelo(String ruta) throws Exception {
        if (!modelosCargados.containsKey(ruta)) {
            modelosCargados.put(ruta, (Classifier) SerializationHelper.read(ruta));
        }
        return modelosCargados.get(ruta);
    }

    private static String predecir(Classifier cls, Instances estructura, double[] vals) throws Exception {
        DenseInstance inst = new DenseInstance(1.0, vals);
        inst.setDataset(estructura);
        return estructura.classAttribute().value((int) cls.classifyInstance(inst));
    }

    private static double[] extraerValoresSpam(Mensaje mensaje, Instances estructura) {
        String as = (mensaje.getAsunto() != null ? mensaje.getAsunto() : "").toLowerCase();
        String cu = (mensaje.getCuerpo() != null ? mensaje.getCuerpo() : "").toLowerCase();
        return new double[]{as.length(), cu.length(), 
            as.contains("oferta") || cu.contains("oferta") ? 1.0 : 0.0,
            as.contains("gratis") || cu.contains("gratis") ? 1.0 : 0.0,
            as.contains("%") || cu.contains("%") ? 1.0 : 0.0, 0.0};
    }

    private static double[] extraerValoresPrioridad(Mensaje mensaje, Instances estructura) {
        String as = (mensaje.getAsunto() != null ? mensaje.getAsunto() : "").toLowerCase();
        String cu = (mensaje.getCuerpo() != null ? mensaje.getCuerpo() : "").toLowerCase();
        return new double[]{0.0, cu.contains("?") ? 1.0 : 0.0, 
            as.startsWith("re:") ? 1.0 : 0.0, cu.length(), 0.0};
    }
}