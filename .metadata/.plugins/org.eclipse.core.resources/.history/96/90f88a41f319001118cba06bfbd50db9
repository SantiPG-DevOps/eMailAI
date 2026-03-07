package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOEntrenamiento;
import com.emailAI.dao.DAOMensajes;
import com.emailAI.ia.ExtractorAtributos;
import com.emailAI.ia.GestorModelos;
import com.emailAI.model.Mensaje;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.MailService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CorreoController {

    @FXML private ListView<String> lstMensajes;
    @FXML private Label lblAsunto;
    @FXML private Label lblRemitente;
    @FXML private TextArea txtCuerpo;
    @FXML private Label lblEstado;
    @FXML private Button btnSug1, btnSug2, btnSug3;
    @FXML private ProgressIndicator progressCargando; // si no lo tienes en FXML, puedes quitarlo

    private MailService mailService;
    private List<Mensaje> mensajes = new ArrayList<>();

    private DAOMensajes daoMensajes;
    private String cuentaHash; // identificador lógico de la cuenta

    public CorreoController() {
        try {
            daoMensajes = new DAOMensajes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Llamado desde MainController después de que se haya conectado MailService.
     */
    public void setMailService(MailService mailService) throws Exception {
        this.mailService = mailService;

        // 1) Calcular hash de cuenta a partir de email + servidor IMAP
        String email = mailService.getEmail();      // asegúrate de tener getters en MailService
        String imapHost = mailService.getImapHost();
        this.cuentaHash = UtilidadCifrado.hash(email + "@" + imapHost);

        // 2) Cargar mensajes desde BD (rápido, sin red)
        cargarDesdeBD();

        // 3) Lanzar sincronización en segundo plano (no bloquea UI)
        onActualizarBandeja();
    }

    @FXML
    private void initialize() {
        // Listener selección detalle
        lstMensajes.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int idx = newVal.intValue();
            if (idx >= 0 && idx < mensajes.size()) {
                mostrarDetalle(mensajes.get(idx));
            }
        });

        // CellFactory para mostrar cada item como tarjeta
        lstMensajes.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("message-card");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("message-card")) {
                        getStyleClass().add("message-card");
                    }
                }
            }
        });
    }

    // ===================== Carga desde BD =====================

    private void cargarDesdeBD() {
        if (cuentaHash == null || daoMensajes == null) return;

        try {
            mensajes = daoMensajes.listarPorCuenta(cuentaHash);
            actualizarListaVisual();
            lblEstado.setText("Bandeja local cargada.");
            if (!mensajes.isEmpty()) {
                lstMensajes.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            lblEstado.setText("Error al cargar bandeja local: " + e.getMessage());
        }
    }

    // ===================== Sincronización en segundo plano =====================

    @FXML
    private void onActualizarBandeja() {
        if (mailService == null) return;

        lblEstado.setText("Sincronizando correos...");
        if (progressCargando != null) progressCargando.setVisible(true);

        Task<List<Mensaje>> tareaDescarga = new Task<>() {
            @Override
            protected List<Mensaje> call() throws Exception {
                // 1) Descargar de IMAP
                List<Mensaje> descargados = mailService.listInbox();

                // 2) Guardar/actualizar en BD
                if (cuentaHash != null && daoMensajes != null) {
                    daoMensajes.guardarOModificar(cuentaHash, descargados);
                }

                return descargados;
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
            lblEstado.setText("Error al sincronizar correos (modo offline).");
            tareaDescarga.getException().printStackTrace();
        });

        Thread hilo = new Thread(tareaDescarga);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarListaVisual() {
        var items = FXCollections.<String>observableArrayList();
        for (Mensaje m : mensajes) {
            String linea1 = m.getRemitente();
            String prefijo = m.getCategoria() != null && m.getCategoria().equals("SPAM") ? "[SPAM]" : "";
            if ("URGENTE".equals(m.getPrioridad())) {
                prefijo = "[URGENTE]";
            }
            String linea2 = (prefijo + " " + m.getAsunto()).trim();
            items.add(linea1 + "\n" + linea2);
        }
        lstMensajes.setItems(items);
    }

    private void mostrarDetalle(Mensaje m) {
        lblAsunto.setText(m.getAsunto());
        lblRemitente.setText("De: " + m.getRemitente());
        txtCuerpo.setText(m.getCuerpo());
        generarSugerencias(m);
    }

    // ===== Lógica de Entrenamiento con el nuevo DAO (como tenías) =====

    @FXML private void onMarcarSpam()      { marcarConEtiqueta("SPAM", "SPAM"); }
    @FXML private void onMarcarLegitimo()  { marcarConEtiqueta("LEGITIMO", "SPAM"); }
    @FXML private void onMarcarUrgente()   { marcarConEtiqueta("URGENTE", "PRIORIDAD"); }

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

                var eSpam = dao.listarEjemplosPorTipo("SPAM");
                if (!eSpam.isEmpty()) {
                    GestorModelos.entrenarYGuardar(ExtractorAtributos.convertirAEstructura(eSpam), "SPAM");
                }

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

    // ===== Lógica de Sugerencias y botones =====

    private void generarSugerencias(Mensaje m) {
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
            abrirCompose(m.getRemitente(), "Re: " + m.getAsunto(),
                    "\n\n--- Original ---\n" + m.getCuerpo());
        }
    }

    @FXML
    private void onReenviar() {
        int idx = lstMensajes.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            Mensaje m = mensajes.get(idx);
            abrirCompose(null, "Fwd: " + m.getAsunto(),
                    "\n\n--- Mensaje reenviado ---\n" + m.getCuerpo());
        } else {
            lblEstado.setText("Selecciona un mensaje para reenviar.");
        }
    }

    // Botones de respuesta rápida IA
    @FXML private void onUsarRespuesta1() { usarRespuestaRapida(btnSug1); }
    @FXML private void onUsarRespuesta2() { usarRespuestaRapida(btnSug2); }
    @FXML private void onUsarRespuesta3() { usarRespuestaRapida(btnSug3); }

    private void usarRespuestaRapida(Button boton) {
        int idx = lstMensajes.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            lblEstado.setText("Selecciona un mensaje primero.");
            return;
        }
        Mensaje m = mensajes.get(idx);
        String texto = boton.getText();
        abrirCompose(m.getRemitente(), "Re: " + m.getAsunto(),
                texto + "\n\n--- Mensaje original ---\n" + m.getCuerpo());
    }
}
