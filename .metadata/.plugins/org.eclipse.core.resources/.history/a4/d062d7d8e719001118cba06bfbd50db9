package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.security.UtilidadCifrado;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigController {

    @FXML
    private ComboBox<String> providerCombo;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField masterPassField;

    @FXML
    private TextField imapField;

    @FXML
    private TextField smtpField;

    @FXML
    private Label imapLabel;

    @FXML
    private Label smtpLabel;

    @FXML
    private Label statusLabel;

    // Mapa proveedor -> (imap,smtp)
    private final Map<String, String[]> proveedores = new HashMap<>();

    private static final String PERSONALIZADO = "Servidor personalizado";

    @FXML
    private void initialize() {
        statusLabel.setText("");

        // 1. Rellenar lista de proveedores más usados (puedes ajustar la lista)
        proveedores.put("Gmail",      new String[]{"imap.gmail.com", "smtp.gmail.com"});
        proveedores.put("Outlook/Hotmail", new String[]{"imap-mail.outlook.com", "smtp-mail.outlook.com"});
        proveedores.put("Yahoo",      new String[]{"imap.mail.yahoo.com", "smtp.mail.yahoo.com"});
        proveedores.put("iCloud",     new String[]{"imap.mail.me.com", "smtp.mail.me.com"});
        proveedores.put("GMX",        new String[]{"imap.gmx.com", "mail.gmx.com"});
        proveedores.put("ProtonMail (Bridge)", new String[]{"127.0.0.1", "127.0.0.1"});
        proveedores.put("Zoho Mail",  new String[]{"imap.zoho.com", "smtp.zoho.com"});

        // Añadir al ComboBox
        providerCombo.getItems().addAll(
                "Gmail",
                "Outlook/Hotmail",
                "Yahoo",
                "iCloud",
                "GMX",
                "ProtonMail (Bridge)",
                "Zoho Mail",
                PERSONALIZADO
        );

        providerCombo.getSelectionModel().selectFirst(); // por defecto Gmail
        aplicarProveedorSeleccionado();
    }

    @FXML
    private void onProviderChanged(ActionEvent event) {
        aplicarProveedorSeleccionado();
    }

    private void aplicarProveedorSeleccionado() {
        String proveedor = providerCombo.getSelectionModel().getSelectedItem();
        if (proveedor == null) return;

        if (PERSONALIZADO.equals(proveedor)) {
            // Mostrar campos IMAP/SMTP vacíos y editables
            imapLabel.setVisible(true);
            imapField.setVisible(true);
            smtpLabel.setVisible(true);
            smtpField.setVisible(true);

            imapField.clear();
            smtpField.clear();
            imapField.setEditable(true);
            smtpField.setEditable(true);
        } else {
            // Rellenar automáticamente y ocultar campos
            String[] datos = proveedores.get(proveedor);
            String imap = datos[0];
            String smtp = datos[1];

            imapField.setText(imap);
            smtpField.setText(smtp);

            imapLabel.setVisible(false);
            imapField.setVisible(false);
            smtpLabel.setVisible(false);
            smtpField.setVisible(false);
        }
    }

    @FXML
    private void onSaveClicked(ActionEvent event) {
        statusLabel.setText("");

        String proveedor = providerCombo.getSelectionModel().getSelectedItem();
        String email = emailField.getText();
        String masterPass = masterPassField.getText();

        // Resolver IMAP/SMTP según proveedor
        String imap;
        String smtp;

        if (proveedor == null) {
            statusLabel.setText("Selecciona un proveedor.");
            return;
        }

        if (PERSONALIZADO.equals(proveedor)) {
            imap = imapField.getText();
            smtp = smtpField.getText();
        } else {
            String[] datos = proveedores.get(proveedor);
            imap = datos[0];
            smtp = datos[1];
        }

        // Validaciones básicas
        if (email == null || email.isBlank()
                || masterPass == null || masterPass.isBlank()
                || imap == null || imap.isBlank()
                || smtp == null || smtp.isBlank()) {
            statusLabel.setText("Rellena todos los campos necesarios.");
            return;
        }

        try {
            String emailCifrado = UtilidadCifrado.cifrar(email);
            String passHash = UtilidadCifrado.hash(masterPass);

            DAOCuentas dao = new DAOCuentas();
            dao.guardarCuenta(imap, smtp, emailCifrado, passHash);

            statusLabel.setText("Cuenta guardada correctamente.");
            irALogin(event);

        } catch (Exception e) {
            statusLabel.setText("Error guardando cuenta: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelClicked(ActionEvent event) {
        try {
            irALogin(event);
        } catch (Exception e) {
            statusLabel.setText("Error al volver al login: " + e.getMessage());
        }
    }

    private void irALogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/login-view.fxml"));
        Scene loginScene = new Scene(loader.load());

        Scene currentScene = ((Node) event.getSource()).getScene();
        if (currentScene != null && !currentScene.getStylesheets().isEmpty()) {
            loginScene.getStylesheets().setAll(currentScene.getStylesheets());
        } else {
            loginScene.getStylesheets().add(
                    AppFX.class.getResource("/styles-dark.css").toExternalForm()
            );
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("eMailAI - Login");
        stage.setScene(loginScene);
        stage.show();
    }
}
