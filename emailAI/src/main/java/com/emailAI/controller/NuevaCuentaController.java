package com.emailAI.controller;

import com.emailAI.dao.DAOCuentas;
import com.emailAI.security.UtilidadCifrado;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NuevaCuentaController {

    @FXML
    private TextField servidorImapField;

    @FXML
    private TextField servidorSmtpField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passCorreoField;

    @FXML
    private PasswordField passMaestraField;

    @FXML
    private PasswordField passMaestraRepeatField;

    @FXML
    private Label statusLabel;

    @FXML
    private void onGuardarClicked(ActionEvent event) {
        statusLabel.setText("");

        String servidorImap = servidorImapField.getText();
        String servidorSmtp = servidorSmtpField.getText();
        String email = emailField.getText();
        String passCorreo = passCorreoField.getText();
        String passMaestra = passMaestraField.getText();
        String passMaestra2 = passMaestraRepeatField.getText();

        if (servidorImap.isBlank() || servidorSmtp.isBlank()
                || email.isBlank() || passCorreo.isBlank()
                || passMaestra.isBlank() || passMaestra2.isBlank()) {
            statusLabel.setText("Rellena todos los campos.");
            return;
        }

        if (!passMaestra.equals(passMaestra2)) {
            statusLabel.setText("La contraseña maestra no coincide.");
            return;
        }

        try {
            String emailCifrado = UtilidadCifrado.cifrar(email);
            String passCorreoCifrada = UtilidadCifrado.cifrar(passCorreo);
            String passMaestraHash = UtilidadCifrado.hash(passMaestra);

            DAOCuentas dao = new DAOCuentas();
            dao.guardarCuenta(
                    servidorImap,
                    servidorSmtp,
                    emailCifrado,
                    passMaestraHash,
                    passCorreoCifrada
            );

            cerrarVentana(event);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error guardando cuenta: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        cerrarVentana(event);
    }

    private void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
