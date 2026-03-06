package com.emailAI.controller;

import com.emailAI.service.MailService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ComposeController {

    @FXML private TextField txtPara;
    @FXML private TextField txtAsunto;
    @FXML private TextArea txtCuerpo;
    @FXML private Label lblEstado;

    private MailService mailService;

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    // Para responder/reenviar más adelante
    public void precargarDatos(String para, String asunto, String cuerpo) {
        if (para != null) txtPara.setText(para);
        if (asunto != null) txtAsunto.setText(asunto);
        if (cuerpo != null) txtCuerpo.setText(cuerpo);
    }

    @FXML
    private void onEnviar() {
        String to = txtPara.getText().trim();
        String subject = txtAsunto.getText().trim();
        String body = txtCuerpo.getText();

        if (to.isEmpty() || subject.isEmpty()) {
            lblEstado.setText("Para y Asunto son obligatorios.");
            return;
        }

        try {
            mailService.sendEmail(to, subject, body);
            lblEstado.setText("Correo enviado correctamente.");

            Stage stage = (Stage) txtPara.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            lblEstado.setText("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
