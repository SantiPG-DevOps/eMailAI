package com.emailAI.controller;

import com.emailAI.model.Mensaje;
import com.emailAI.service.MailService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ComposeController {

    @FXML private TextField txtPara;
    @FXML private TextField txtCc;
    @FXML private TextField txtAsunto;
    @FXML private TextArea txtCuerpo;
    @FXML private Label lblEstado;

    private MailService mailService;
    private Stage stage;
    private Mensaje mensajeOrigen;

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ==== Modos de uso ====

    public void inicializarNuevo() {
        mensajeOrigen = null;
        txtPara.clear();
        txtCc.clear();
        txtAsunto.clear();
        txtCuerpo.clear();
        lblEstado.setText("");
    }

    public void inicializarResponder(Mensaje origen) {
        mensajeOrigen = origen;
        txtPara.setText(origen.getRemitente());
        txtCc.clear();
        txtAsunto.setText("Re: " + safe(origen.getAsunto()));
        txtCuerpo.setText("\n\n----- Mensaje original -----\n" + safe(origen.getCuerpo()));
        lblEstado.setText("");
    }

    public void inicializarResponderTodos(Mensaje origen) {
        mensajeOrigen = origen;
        txtPara.setText(origen.getRemitente());
        txtCc.clear(); // Más adelante: añadir resto de destinatarios
        txtAsunto.setText("Re: " + safe(origen.getAsunto()));
        txtCuerpo.setText("\n\n----- Mensaje original -----\n" + safe(origen.getCuerpo()));
        lblEstado.setText("");
    }

    public void inicializarReenviar(Mensaje origen) {
        mensajeOrigen = origen;
        txtPara.clear();
        txtCc.clear();
        txtAsunto.setText("Fwd: " + safe(origen.getAsunto()));
        txtCuerpo.setText(
                "\n\n----- Mensaje reenviado -----\n" +
                "De: " + safe(origen.getRemitente()) + "\n" +
                "Asunto: " + safe(origen.getAsunto()) + "\n\n" +
                safe(origen.getCuerpo())
        );
        lblEstado.setText("");
    }

    // ==== Acciones ====

    @FXML
    private void onEnviar() {
        if (mailService == null) {
            lblEstado.setText("Error: MailService no disponible.");
            return;
        }

        String to = txtPara.getText() != null ? txtPara.getText().trim() : "";
        String subject = txtAsunto.getText() != null ? txtAsunto.getText().trim() : "";
        String body = txtCuerpo.getText() != null ? txtCuerpo.getText() : "";

        if (to.isEmpty()) {
            lblEstado.setText("El campo 'Para' es obligatorio.");
            return;
        }

        if (subject.isEmpty()) {
            lblEstado.setText("El campo 'Asunto' es obligatorio.");
            return;
        }

        try {
            // De momento solo TO; más adelante ampliamos MailService para CC/BCC.
            mailService.sendEmail(to, subject, body);
            lblEstado.setText("Correo enviado correctamente.");

            if (stage == null && txtPara.getScene() != null) {
                stage = (Stage) txtPara.getScene().getWindow();
            }
            if (stage != null) {
                stage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("Error al enviar: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        if (stage == null && txtPara.getScene() != null) {
            stage = (Stage) txtPara.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}