package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.security.UtilidadCifrado;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.Map;

public class ConfigController {

    // ====== Pestaña GENERAL ======

    @FXML private TabPane tabPrincipal;
    @FXML private CheckBox chkTemaClaro;
    @FXML private CheckBox chkNotificaciones;
    @FXML private CheckBox chkComprobarInicio;

    // ====== Pestaña CORREO (config avanzada de cuentas) ======

    @FXML private ComboBox<String> providerCombo;
    @FXML private TextField emailField;
    @FXML private PasswordField masterPassField;
    @FXML private TextField imapField;
    @FXML private TextField smtpField;
    @FXML private Label imapLabel;
    @FXML private Label smtpLabel;
    @FXML private Label statusLabel;

    private final Map<String, String[]> proveedores = new HashMap<>();
    private static final String PERSONALIZADO = "Servidor personalizado";

    // referencia opcional al MainController (cuando se abre desde la app principal)
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        // inicializar estado del tema según la ventana principal
        if (mainController != null && chkTemaClaro != null) {
            chkTemaClaro.setSelected(mainController.isTemaClaro());
        } else if (chkTemaClaro != null) {
            // si se abre desde otra escena, deducimos el tema por CSS
            Scene scene = chkTemaClaro.getScene();
            boolean light = false;
            if (scene != null) {
                for (String s : scene.getStylesheets()) {
                    if (s.contains("styles-light.css")) {
                        light = true;
                        break;
                    }
                }
            }
            chkTemaClaro.setSelected(light);
        }
    }

    @FXML
    private void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        // === Inicialización de proveedores (pestaña Correo) ===
        if (providerCombo != null) {
            proveedores.put("Gmail",      new String[]{"imap.gmail.com", "smtp.gmail.com"});
            proveedores.put("Outlook/Hotmail", new String[]{"imap-mail.outlook.com", "smtp-mail.outlook.com"});
            proveedores.put("Yahoo",      new String[]{"imap.mail.yahoo.com", "smtp.mail.yahoo.com"});
            proveedores.put("iCloud",     new String[]{"imap.mail.me.com", "smtp.mail.me.com"});
            proveedores.put("GMX",        new String[]{"imap.gmx.com", "mail.gmx.com"});
            proveedores.put("ProtonMail (Bridge)", new String[]{"127.0.0.1", "127.0.0.1"});
            proveedores.put("Zoho Mail",  new String[]{"imap.zoho.com", "smtp.zoho.com"});

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

            providerCombo.getSelectionModel().selectFirst();
            aplicarProveedorSeleccionado();
        }
    }

    // ====== General: tema claro/oscuro ======

    @FXML
    private void onToggleTemaClaro() {
        if (chkTemaClaro == null) return;
        boolean claro = chkTemaClaro.isSelected();

        if (mainController != null) {
            mainController.setTemaClaro(claro);
        } else {
            Scene scene = chkTemaClaro.getScene();
            if (scene == null) return;

            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-basic.css").toExternalForm()
            );
            if (claro) {
                scene.getStylesheets().add(
                        AppFX.class.getResource("/styles-light.css").toExternalForm()
                );
            } else {
                scene.getStylesheets().add(
                        AppFX.class.getResource("/styles-dark.css").toExternalForm()
                );
            }
        }
    }

    // ====== Correo: proveedor / IMAP / SMTP ======

    @FXML
    private void onProviderChanged() {
        aplicarProveedorSeleccionado();
    }

    private void aplicarProveedorSeleccionado() {
        if (providerCombo == null) return;

        String proveedor = providerCombo.getSelectionModel().getSelectedItem();
        if (proveedor == null) return;

        if (PERSONALIZADO.equals(proveedor)) {
            if (imapLabel != null) imapLabel.setVisible(true);
            if (imapField != null) {
                imapField.setVisible(true);
                imapField.clear();
                imapField.setEditable(true);
            }

            if (smtpLabel != null) smtpLabel.setVisible(true);
            if (smtpField != null) {
                smtpField.setVisible(true);
                smtpField.clear();
                smtpField.setEditable(true);
            }
        } else {
            String[] datos = proveedores.get(proveedor);
            if (datos == null) return;

            String imap = datos[0];
            String smtp = datos[1];

            if (imapField != null) {
                imapField.setText(imap);
                imapField.setVisible(false);
            }
            if (smtpField != null) {
                smtpField.setText(smtp);
                smtpField.setVisible(false);
            }
            if (imapLabel != null) imapLabel.setVisible(false);
            if (smtpLabel != null) smtpLabel.setVisible(false);
        }
    }

    @FXML
    private void onSaveCuenta() {
        if (statusLabel != null) statusLabel.setText("");

        if (providerCombo == null) return;

        String proveedor = providerCombo.getSelectionModel().getSelectedItem();
        String email = emailField != null ? emailField.getText() : null;
        String masterPass = masterPassField != null ? masterPassField.getText() : null;

        String imap;
        String smtp;

        if (proveedor == null) {
            if (statusLabel != null) statusLabel.setText("Selecciona un proveedor.");
            return;
        }

        if (PERSONALIZADO.equals(proveedor)) {
            imap = imapField != null ? imapField.getText() : null;
            smtp = smtpField != null ? smtpField.getText() : null;
        } else {
            String[] datos = proveedores.get(proveedor);
            imap = datos[0];
            smtp = datos[1];
        }

        if (email == null || email.isBlank()
                || masterPass == null || masterPass.isBlank()
                || imap == null || imap.isBlank()
                || smtp == null || smtp.isBlank()) {
            if (statusLabel != null) statusLabel.setText("Rellena todos los campos necesarios.");
            return;
        }

        try {
            // email cifrado con clave fija de app
            String emailCifrado = UtilidadCifrado.cifrar(email);
            // hash de la contraseña maestra (igual que en NuevaCuentaController)
            String passMaestraHash = UtilidadCifrado.hash(masterPass);

            DAOCuentas dao = new DAOCuentas();
            dao.guardarCuenta(imap, smtp, emailCifrado, passMaestraHash);

            if (statusLabel != null) statusLabel.setText("Cuenta guardada correctamente.");
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Error guardando cuenta: " + e.getMessage());
        }
    }
}
