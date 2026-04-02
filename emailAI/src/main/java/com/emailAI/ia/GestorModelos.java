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

// Orquesta entrenamiento, carga en caché y predicción de modelos Weka.
public class GestorModelos {

    private static final Path DIR_IA = Path.of("db", "ia");
    private static final Path RUTA_MODELO_SPAM = DIR_IA.resolve("modelo-spam.model");
    private static final Path RUTA_MODELO_PRIORIDAD = DIR_IA.resolve("prioridad.model");

    // Caché en memoria para no leer del disco cada vez.
    private static final Map<String, Classifier> modelosCargados = new HashMap<>();

    /**
     * Entrena y guarda un modelo según el tipo: "SPAM" o "PRIORIDAD".
     */
    public static void entrenarYGuardar(Instances data, String tipo) throws Exception {
        if (data == null || data.numInstances() < 5) {
            throw new IllegalArgumentException("Faltan datos para entrenar el modelo");
        }

        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        Files.createDirectories(DIR_IA);

        Classifier cls = new NaiveBayes();
        cls.buildClassifier(data);

        Path ruta = obtenerRutaModelo(tipo);
        SerializationHelper.write(ruta.toString(), cls);
        modelosCargados.put(ruta.toString(), cls);
    }

    /**
     * Clasifica un mensaje como SPAM/LEGITIMO/DESCONOCIDO.
     */
    public static String clasificarSpam(Mensaje mensaje) throws Exception {
        if (!Files.exists(RUTA_MODELO_SPAM)) {
            return "DESCONOCIDO";
        }

        Classifier cls = obtenerModelo(RUTA_MODELO_SPAM);
        Instances estructura = ExtractorAtributos.construirEstructura();

        if (estructura.classIndex() == -1) {
            estructura.setClassIndex(estructura.numAttributes() - 1);
        }

        double[] vals = extraerValoresSpam(mensaje);
        validarNumeroAtributos(estructura, vals);

        return predecir(cls, estructura, vals);
    }

    /**
     * Clasifica la prioridad de un mensaje.
     */
    public static String clasificarPrioridad(Mensaje mensaje) throws Exception {
        if (!Files.exists(RUTA_MODELO_PRIORIDAD)) {
            return "NORMAL";
        }

        Classifier cls = obtenerModelo(RUTA_MODELO_PRIORIDAD);
        Instances estructura = ExtractorAtributos.construirEstructuraPrioridad();

        if (estructura.classIndex() == -1) {
            estructura.setClassIndex(estructura.numAttributes() - 1);
        }

        double[] vals = extraerValoresPrioridad(mensaje);
        validarNumeroAtributos(estructura, vals);

        return predecir(cls, estructura, vals);
    }

    private static Path obtenerRutaModelo(String tipo) {
        if ("SPAM".equalsIgnoreCase(tipo)) {
            return RUTA_MODELO_SPAM;
        }
        if ("PRIORIDAD".equalsIgnoreCase(tipo)) {
            return RUTA_MODELO_PRIORIDAD;
        }
        throw new IllegalArgumentException("Tipo de modelo desconocido: " + tipo);
    }

    private static Classifier obtenerModelo(Path ruta) throws Exception {
        String clave = ruta.toString();

        if (!modelosCargados.containsKey(clave)) {
            modelosCargados.put(clave, (Classifier) SerializationHelper.read(clave));
        }

        return modelosCargados.get(clave);
    }

    private static String predecir(Classifier cls, Instances estructura, double[] vals) throws Exception {
        DenseInstance inst = new DenseInstance(1.0, vals);
        inst.setDataset(estructura);

        double idxClase = cls.classifyInstance(inst);
        return estructura.classAttribute().value((int) idxClase);
    }

    private static void validarNumeroAtributos(Instances estructura, double[] vals) {
        int esperados = estructura.numAttributes() - 1;
        if (vals.length != esperados) {
            throw new IllegalArgumentException(
                    "Número de atributos incorrecto. Esperados: " + esperados + ", recibidos: " + vals.length
            );
        }
    }

    /**
     * Debe coincidir EXACTAMENTE con ExtractorAtributos.construirEstructura().
     */
    private static double[] extraerValoresSpam(Mensaje mensaje) {
        String as = mensaje.getAsunto() != null ? mensaje.getAsunto().toLowerCase() : "";
        String cu = mensaje.getCuerpo() != null ? mensaje.getCuerpo().toLowerCase() : "";

        return new double[] {
                as.length(),                                                // longitud_asunto
                cu.length(),                                                // longitud_cuerpo
                (as.contains("oferta") || cu.contains("oferta")) ? 1.0 : 0.0,
                (as.contains("gratis") || cu.contains("gratis")) ? 1.0 : 0.0,
                (as.contains("%") || cu.contains("%")) ? 1.0 : 0.0
        };
    }

    /**
     * Debe coincidir EXACTAMENTE con ExtractorAtributos.construirEstructuraPrioridad().
     */
    private static double[] extraerValoresPrioridad(Mensaje mensaje) {
        String as = mensaje.getAsunto() != null ? mensaje.getAsunto().toLowerCase() : "";
        String cu = mensaje.getCuerpo() != null ? mensaje.getCuerpo().toLowerCase() : "";

        return new double[] {
                0.0,                               // es_remitente_frecuente (placeholder)
                (cu.contains("?") || cu.contains("¿")) ? 1.0 : 0.0,
                as.startsWith("re:") ? 1.0 : 0.0,
                cu.length()
        };
    }
}