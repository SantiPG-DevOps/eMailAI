package com.emailAI.service;

import com.emailAI.model.Mensaje;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Implementa un modelo Naive Bayes con bag-of-words para clasificar correos como LEGITIMO/SPAM/PHISHING.
public class SpamIaService {

    private final Path modelosDir;

    // atributos Weka compartidos
    private final ArrayList<Attribute> atributos;
    private final Attribute attrTexto;
    private final Attribute attrClase;

    public enum ClaseCorreo {
        LEGITIMO, SPAM, PHISHING
    }

    // Prepara el directorio de modelos y define el esquema de atributos Weka compartido.
    public SpamIaService(Path modelosDir) throws IOException {
        this.modelosDir = modelosDir;
        if (!Files.exists(modelosDir)) {
            Files.createDirectories(modelosDir);
        }

        atributos = new ArrayList<>();

        // atributo de texto
        attrTexto = new Attribute("texto", (List<String>) null);
        atributos.add(attrTexto);

        // atributo clase
        ArrayList<String> clases = new ArrayList<>();
        clases.add("LEGITIMO");
        clases.add("SPAM");
        clases.add("PHISHING");
        attrClase = new Attribute("clase", clases);
        atributos.add(attrClase);
    }

    // Crea un dataset vacío con los atributos configurados y marca la clase.
    private Instances crearDatasetVacio(String nombre) {
        Instances data = new Instances(nombre, atributos, 0);
        data.setClass(attrClase);
        return data;
    }

    // Construye un clasificador NaiveBayes envuelto en un filtro StringToWordVector.
    private FilteredClassifier crearClasificadorBase() {
        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("first"); // el texto es el primer atributo

        Classifier base = new NaiveBayes();
        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(filter);
        fc.setClassifier(base);
        return fc;
    }

    // Calcula la ruta de fichero donde se guarda el modelo de una cuenta concreta.
    private Path modeloPath(String cuentaHash) {
        return modelosDir.resolve("modelo_" + cuentaHash + ".model");
    }

    // ================== ENTRENAR / REENTRENAR ==================

    // Entrena (o reentrena) el modelo de una cuenta con la lista de mensajes etiquetados.
    public void entrenarModelo(String cuentaHash, List<Mensaje> ejemplos) throws Exception {
        if (cuentaHash == null || ejemplos == null || ejemplos.isEmpty()) return;

        Instances data = crearDatasetVacio("correos_" + cuentaHash);

        for (Mensaje m : ejemplos) {
            if (m.getCategoria() == null) continue; // necesitamos etiqueta previa

            String texto = (m.getCuerpo() != null ? m.getCuerpo() : "") +
                           " " +
                           (m.getAsunto() != null ? m.getAsunto() : "");

            DenseInstance inst = new DenseInstance(2);
            inst.setValue(attrTexto, texto);
            inst.setValue(attrClase, m.getCategoria().toUpperCase());
            data.add(inst);
        }

        if (data.isEmpty()) return;

        FilteredClassifier fc = crearClasificadorBase();
        fc.buildClassifier(data);

        // guardar modelo a disco
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(
                        Files.newOutputStream(modeloPath(cuentaHash))))) {
            oos.writeObject(fc);
        }
    }

    // ================== CLASIFICAR UN MENSAJE ==================

    // Carga el modelo de una cuenta y devuelve la clase predicha para un mensaje.
    public ClaseCorreo clasificar(String cuentaHash, Mensaje mensaje) throws Exception {
        Path path = modeloPath(cuentaHash);
        if (!Files.exists(path)) {
            return ClaseCorreo.LEGITIMO; // sin modelo → por defecto
        }

        FilteredClassifier fc;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(
                        Files.newInputStream(path)))) {
            fc = (FilteredClassifier) ois.readObject();
        }

        Instances data = crearDatasetVacio("test_" + cuentaHash);

        String texto = (mensaje.getCuerpo() != null ? mensaje.getCuerpo() : "") +
                       " " +
                       (mensaje.getAsunto() != null ? mensaje.getAsunto() : "");

        DenseInstance inst = new DenseInstance(2);
        inst.setDataset(data);
        inst.setValue(attrTexto, texto);
        data.add(inst);

        double idx = fc.classifyInstance(inst);
        String etiqueta = data.classAttribute().value((int) idx);

        return ClaseCorreo.valueOf(etiqueta);
    }

    // ================== UTILIDAD RÁPIDA ==================

    // Elimina el fichero de modelo asociado a una cuenta concreta si existe.
    public void borrarModelo(String cuentaHash) throws IOException {
        Path p = modeloPath(cuentaHash);
        if (Files.exists(p)) {
            Files.delete(p);
        }
    }
}