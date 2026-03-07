package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOEntrenamiento;
import com.emailAI.ia.ExtractorAtributos;
import com.emailAI.ia.GestorModelos;
import com.emailAI.model.Mensaje;
import com.emailAI.service.MailService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class CorreoController {

    @FXML private ListView<String> lstMensajes;
    @FXML private Label lblAsunto;
    @FXML private Label lblRemitente;
    @FXML private TextArea txtCuerpo;
    @FXML private Label lblEstado;
    @FXML private Button btnSug1, btnSug2, btnSug3;
    
    // Nuevo: Indicador visual para que el usuario sepa que la app trabaja
    @FXML private ProgressIndicator progressCargando;

    private MailService mailService;
    private List<Mensaje> mensajes;

    public void setMailService(MailService mailService) throws Exception {
        this.mailService = mailService;
        onActualizarBandeja(); // Inicia la carga asíncrona al entrar
    }

    @FXML
    private void initialize() {
        // Listener para el detalle del mensaje
        lstMensajes.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int idx = newVal.intValue();
            if (idx >= 0 && idx < mensajes.size()) {
                mostrarDetalle(mensajes.get(idx));
            }
        });
    }

    /**
     * Lógica Asíncrona: Descarga correos sin bloquear la UI
     */
    @FXML
    private void onActualizarBandeja() {
        if (mailService == null) return;

        lblEstado.setText("Sincronizando correos...");
        if (progressCargando != null) progressCargando.setVisible(true);

        Task<List<Mensaje>> tareaDescarga = new Task<>() {
            @Override
            protected List<Mensaje> call() throws Exception {
                // Esto ocurre en un hilo secundario
                return mailService.listInbox();
            }
        };

        tareaDescarga.setOnSucceeded(event -> {
            mensajes = tareaDescarga.getValue();
            actualizarListaVisual();
            if (progressCargando != null) progressCargando.setVisible(false);
            lblEstado.setText("Bandeja actualizada.");
            
            if (!mensajes.isEmpty()) {
                lstMensajes.getSelectionModel().select(0);
            }
        });

        tareaDescarga.setOnFailed(event -> {
            if (progressCargando != null) progressCargando.setVisible(false);
            lblEstado.setText("Error al sincronizar correos.");
            tareaDescarga.getException().printStackTrace();
        });

        Thread hilo = new Thread(tareaDescarga);
        hilo.setDaemon(true); 
        hilo.start();
    }

    private void actualizarListaVisual() {
        var items = FXCollections.<String>observableArrayList();
        for (Mensaje m : mensajes) {
            // Iconografía profesional según clasificación
            String prefijo = m.getCategoria().equals("SPAM") ? "[🚫 SPAM]" : "[✉️]";
            if ("URGENTE".equals(m.getPrioridad())) {
                prefijo = "[🔥 URGENTE]";
            }
            items.add(prefijo + " " + m.getAsunto());
        }
        lstMensajes.setItems(items);
    }

    private void mostrarDetalle(Mensaje m) {
        lblAsunto.setText(m.getAsunto());
        lblRemitente.setText("De: " + m.getRemitente());
        txtCuerpo.setText(m.getCuerpo());
        generarSugerencias(m);
    }

    // ===== Lógica de Entrenamiento con el nuevo DAO (3 parámetros) =====

    @FXML private void onMarcarSpam() { marcarConEtiqueta("SPAM", "SPAM"); }
    @FXML private void onMarcarLegitimo() { marcarConEtiqueta("LEGITIMO", "SPAM"); }
    @FXML private void onMarcarUrgente() { marcarConEtiqueta("URGENTE", "PRIORIDAD"); }

    private void marcarConEtiqueta(String etiqueta, String tipoModelo) {
        int idx = lstMensajes.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;

        Mensaje m = mensajes.get(idx);
        try {
            DAOEntrenamiento dao = new DAOEntrenamiento();
            dao.guardarEjemplo(m, etiqueta, tipoModelo);
            lblEstado.setText("Aprendido: " + etiqueta + " en " + tipoModelo);
        } catch (Exception e) {
            lblEstado.setText("Error al guardar entrenamiento.");
        }
    }

    @FXML
    private void onReentrenarModelo() {
        lblEstado.setText("Reentrenando IA...");
        Task<Void> tareaEntrenamiento = new Task<>() {
            @Override
            protected Void call() throws Exception {
                DAOEntrenamiento dao = new DAOEntrenamiento();
                
                // Reentrenar Spam
                var eSpam = dao.listarEjemplosPorTipo("SPAM");
                if (!eSpam.isEmpty()) {
                    GestorModelos.entrenarYGuardar(ExtractorAtributos.convertirAEstructura(eSpam), "SPAM");
                }
                
                // Reentrenar Prioridad
                var ePrio = dao.listarEjemplosPorTipo("PRIORIDAD");
                if (!ePrio.isEmpty()) {
                    GestorModelos.entrenarYGuardar(ExtractorAtributos.convertirAEstructuraPrioridad(ePrio), "PRIORIDAD");
                }
                return null;
            }
        };

        tareaEntrenamiento.setOnSucceeded(e -> {
            lblEstado.setText("IA reentrenada con éxito.");
            onActualizarBandeja();
        });

        new Thread(tareaEntrenamiento).start();
    }

    // ===== Lógica de Sugerencias y Navegación =====

    private void generarSugerencias(Mensaje m) {
        // Lógica de botones basada en palabras clave (como tenías antes)
        String texto = (m.getAsunto() + m.getCuerpo()).toLowerCase();
        if (texto.contains("reunión")) {
            btnSug1.setText("Confirmar asistencia");
            btnSug2.setText("No puedo asistir");
            btnSug3.setText("Pedir agenda");
        } else {
            btnSug1.setText("Gracias por el correo");
            btnSug2.setText("Lo reviso luego");
            btnSug3.setText("Necesito más info");
        }
    }

    private void abrirCompose(String para, String asunto, String cuerpo) {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/compose-view.fxml"));
            Scene scene = new Scene(loader.load(), 600, 400);
            ComposeController controller = loader.getController();
            controller.setMailService(mailService);
            if (para != null) controller.precargarDatos(para, asunto, cuerpo);
            
            Stage stage = new Stage();
            stage.setTitle("Redactar");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onRedactar() { abrirCompose(null, null, null); }
    @FXML private void onResponder() { 
        int idx = lstMensajes.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            Mensaje m = mensajes.get(idx);
            abrirCompose(m.getRemitente(), "Re: " + m.getAsunto(), "\n\n--- Original ---\n" + m.getCuerpo());
        }
    }
    @FXML
    private void onReenviar() {
        int idx = lstMensajes.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            Mensaje m = mensajes.get(idx);
            // Llamamos al método de abrir ventana de redactar con el prefijo Fwd:
            abrirCompose(null, "Fwd: " + m.getAsunto(), 
                "\n\n--- Mensaje reenviado ---\n" + m.getCuerpo());
        } else {
            lblEstado.setText("Selecciona un mensaje para reenviar.");
        }
    }
}