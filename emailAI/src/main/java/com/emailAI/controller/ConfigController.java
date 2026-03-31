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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// Controla la pantalla de configuración: tema, alta de cuentas y borrado de cuentas almacenadas.
public class ConfigController {

    // Gestiona los campos de alta de una cuenta de correo nueva.

    // Selector de proveedor (Gmail, Outlook, personalizado, etc.).
    @FXML
    private ComboBox<String> providerCombo;

    // Campo de correo electrónico del usuario a guardar.
    @FXML
    private TextField emailField;

    // Contraseña maestra para proteger la cuenta a nivel local.
    @FXML
    private PasswordField masterPassField;

    // Campo para el host IMAP cuando el proveedor es personalizado.
    @FXML
    private TextField imapField;

    // Campo para el host SMTP cuando el proveedor es personalizado.
    @FXML
    private TextField smtpField;

    // Etiqueta asociada al campo IMAP (se oculta para proveedores conocidos).
    @FXML
    private Label imapLabel;

    // Etiqueta asociada al campo SMTP (se oculta para proveedores conocidos).
    @FXML
    private Label smtpLabel;

    // Muestra errores o mensajes de confirmación en la pantalla de configuración.
    @FXML
    private Label statusLabel;

    // Gestiona los controles de apariencia de la pestaña General.

    // RadioButton para seleccionar tema claro desde la pestaña General.
    @FXML
    private RadioButton rbTemaClaro;

    // RadioButton para seleccionar tema oscuro desde la pestaña General.
    @FXML
    private RadioButton rbTemaOscuro;

    // Gestiona la selección y borrado de cuentas guardadas.

    // Selector de cuentas ya existentes para poder "olvidarlas".
    @FXML
    private ComboBox<String> comboCuentasExistentes;

    private final Map<String, Integer> mapaEmailAId = new HashMap<>();

    // Mantiene los proveedores preconfigurados y sus hosts por defecto.

    private final Map<String, String[]> proveedores = new HashMap<>();
    private static final String PERSONALIZADO = "Servidor personalizado";

    // Conserva referencia al MainController para aplicar cambios de tema globales.

    private MainController mainController; // Referencia opcional al controlador principal para aplicar tema global.

    // Recibe el MainController y sincroniza el estado inicial de los radios de tema.
    public void setMainController(MainController mainController) {
        this.mainController = mainController;

        boolean light = mainController != null && mainController.isTemaClaro();

        if (rbTemaClaro != null && rbTemaOscuro != null) {
            if (light) {
                rbTemaClaro.setSelected(true);
            } else {
                rbTemaOscuro.setSelected(true);
            }
        }
    }

    // Inicializa la pantalla: proveedores por defecto y carga de cuentas existentes.
    @FXML
    private void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        // Carga el catálogo de proveedores soportados con sus hosts conocidos.
        proveedores.put("Gmail",                 new String[]{"imap.gmail.com", "smtp.gmail.com"});
        proveedores.put("Outlook/Hotmail",      new String[]{"imap-mail.outlook.com", "smtp-mail.outlook.com"});
        proveedores.put("Yahoo",                new String[]{"imap.mail.yahoo.com", "smtp.mail.yahoo.com"});
        proveedores.put("iCloud",               new String[]{"imap.mail.me.com", "smtp.mail.me.com"});
        proveedores.put("GMX",                  new String[]{"imap.gmx.com", "mail.gmx.com"});
        proveedores.put("ProtonMail (Bridge)",  new String[]{"127.0.0.1", "127.0.0.1"});
        proveedores.put("Zoho Mail",            new String[]{"imap.zoho.com", "smtp.zoho.com"});

        if (providerCombo != null) {
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

        // Carga la lista de cuentas para el bloque de "Olvidar cuenta".
        cargarCuentasExistentes();
    }

    // Handler cuando se cambia de proveedor para ajustar visibilidad/valores de IMAP/SMTP.
    @FXML
    private void onProviderChanged(ActionEvent event) {
        aplicarProveedorSeleccionado();
    }

    // Rellena o limpia los campos de servidor según el proveedor actualmente elegido.
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
            String imap = datos[0];
            String smtp = datos[1];

            if (imapField != null) {
                imapField.setText(imap);
                imapField.setVisible(false);
            }
            if (imapLabel != null) imapLabel.setVisible(false);

            if (smtpField != null) {
                smtpField.setText(smtp);
                smtpField.setVisible(false);
            }
            if (smtpLabel != null) smtpLabel.setVisible(false);
        }
    }

    // Valida el formulario y guarda la nueva cuenta en la base de datos.
    @FXML
    private void onSaveClicked(ActionEvent event) {
        if (statusLabel != null) statusLabel.setText("");

        String proveedor = providerCombo != null
                ? providerCombo.getSelectionModel().getSelectedItem()
                : null;
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
            String emailCifrado = UtilidadCifrado.cifrar(email);
            String passHash = UtilidadCifrado.hash(masterPass);

            DAOCuentas dao = new DAOCuentas();
            dao.guardarCuenta(imap, smtp, emailCifrado, passHash);

            if (statusLabel != null) statusLabel.setText("Cuenta guardada correctamente.");
            cargarCuentasExistentes();

        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Error guardando cuenta: " + e.getMessage());
        }
    }

    // Rellena el combo de cuentas olvidables leyendo de la base de datos.
    private void cargarCuentasExistentes() {
        if (comboCuentasExistentes == null) return;

        try {
            DAOCuentas dao = new DAOCuentas();
            var cuentas = dao.listarCuentas();

            comboCuentasExistentes.getItems().clear();
            mapaEmailAId.clear();

            for (DAOCuentas.CuentaGuardada c : cuentas) {
                String emailPlano = UtilidadCifrado.descifrar(c.emailCifrado);
                comboCuentasExistentes.getItems().add(emailPlano);
                mapaEmailAId.put(emailPlano, c.id);
            }
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Error cargando cuentas: " + e.getMessage());
        }
    }

    // Elimina de la base de datos la cuenta seleccionada en el combo.
    @FXML
    private void onOlvidarCuentaClicked(ActionEvent event) {
        if (comboCuentasExistentes == null) return;

        String seleccion = comboCuentasExistentes.getSelectionModel().getSelectedItem();
        if (seleccion == null || seleccion.isBlank()) {
            if (statusLabel != null) statusLabel.setText("Selecciona una cuenta para olvidar.");
            return;
        }

        Integer id = mapaEmailAId.get(seleccion);
        if (id == null) {
            if (statusLabel != null) statusLabel.setText("Cuenta no encontrada.");
            return;
        }

        try {
            DAOCuentas dao = new DAOCuentas();
            dao.eliminarCuentaPorId(id);

            if (statusLabel != null) statusLabel.setText("Cuenta olvidada correctamente.");
            cargarCuentasExistentes();
        } catch (SQLException e) {
            if (statusLabel != null) statusLabel.setText("Error al olvidar cuenta: " + e.getMessage());
        }
    }

    // Cambia el tema a claro, usando MainController si está disponible.
    @FXML
    private void onTemaClaro() {
        if (mainController != null) {
            mainController.aplicarTema(true);
        } else {
            aplicarTemaLocal(true);
        }
    }

    // Cambia el tema a oscuro, usando MainController si está disponible.
    @FXML
    private void onTemaOscuro() {
        if (mainController != null) {
            mainController.aplicarTema(false);
        } else {
            aplicarTemaLocal(false);
        }
    }

    // Aplica el tema localmente cuando la vista no recibió MainController.
    private void aplicarTemaLocal(boolean light) {
        Scene scene = rbTemaClaro != null ? rbTemaClaro.getScene() : null;
        if (scene == null) return;

        scene.getStylesheets().clear();
        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-basic.css").toExternalForm()
        );
        if (light) {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-light.css").toExternalForm()
            );
        } else {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-dark.css").toExternalForm()
            );
        }
    }

    // Vuelve a la pantalla de login manteniendo el tema actual.
    @FXML
    private void onCancelClicked(ActionEvent event) {
        try {
            irALogin(event);
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Error al volver al login: " + e.getMessage());
        }
    }

    // Carga la escena de login y le aplica el tema claro u oscuro correspondiente.
    private void irALogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/login-view.fxml"));
        Scene loginScene = new Scene(loader.load());

        loginScene.getStylesheets().add(
                AppFX.class.getResource("/styles-basic.css").toExternalForm()
        );

        Scene currentScene = ((Node) event.getSource()).getScene();
        boolean light = false;
        if (currentScene != null) {
            for (String s : currentScene.getStylesheets()) {
                if (s.contains("styles-light.css")) {
                    light = true;
                    break;
                }
            }
        }

        if (light) {
            loginScene.getStylesheets().add(
                    AppFX.class.getResource("/styles-light.css").toExternalForm()
            );
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