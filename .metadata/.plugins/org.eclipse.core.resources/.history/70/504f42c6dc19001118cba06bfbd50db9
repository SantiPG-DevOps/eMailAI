package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.dao.DAOCuentas.CuentaGuardada;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.MailService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class LoginController {

    @FXML private ComboBox<String> cmbCuentas;
    @FXML private VBox containerProveedor;
    @FXML private ComboBox<String> cmbProveedor;
    @FXML private TextField txtServidor;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblEstado;
    @FXML private Button btnLogin;

    private List<CuentaGuardada> cuentasGuardadas;
    private final MailService mailService = new MailService();

    @FXML
    public void initialize() {
        // Hace que el contenedor no ocupe espacio cuando es invisible
        containerProveedor.managedProperty().bind(containerProveedor.visibleProperty());

        // Configuración de proveedores y servidor automático
        cmbProveedor.setItems(FXCollections.observableArrayList("Gmail", "Outlook", "Yahoo", "Personalizado"));
        cmbProveedor.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            txtServidor.setText(sugerirHost(val));
        });

        cargarCuentas();

        // Lógica de ocultar/mostrar según la cuenta seleccionada
        cmbCuentas.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            boolean esNueva = (val == null || val.equals("Cuenta nueva"));
            containerProveedor.setVisible(esNueva);
            txtEmail.setDisable(!esNueva);
            
            if (!esNueva) {
                txtEmail.setText(val);
                txtPassword.requestFocus();
            } else {
                txtEmail.clear();
            }
        });
    }

    private void cargarCuentas() {
        try {
            DAOCuentas dao = new DAOCuentas();
            cuentasGuardadas = dao.listarCuentas();
            cmbCuentas.getItems().add("Cuenta nueva");
            for (CuentaGuardada c : cuentasGuardadas) {
                cmbCuentas.getItems().add(UtilidadCifrado.descifrar(c.emailCifrado));
            }
            cmbCuentas.getSelectionModel().selectFirst();
        } catch (Exception e) {
            lblEstado.setText("Error DB: " + e.getMessage());
        }
    }

    @FXML
    private void onLogin() {
        String email = txtEmail.getText();
        String pass = txtPassword.getText();
        String cuentaSeleccionada = cmbCuentas.getSelectionModel().getSelectedItem();

        try {
            String imap, smtp;

            if (cuentaSeleccionada.equals("Cuenta nueva")) {
                imap = txtServidor.getText();
                smtp = imap.replace("imap", "smtp");
            } else {
                CuentaGuardada c = cuentasGuardadas.stream()
                    .filter(cg -> {
                        try { return UtilidadCifrado.descifrar(cg.emailCifrado).equals(cuentaSeleccionada); } 
                        catch (Exception e) { return false; }
                    }).findFirst().get();
                imap = c.servidorImap;
                smtp = c.servidorSmtp;
            }

            lblEstado.setText("Conectando...");
            mailService.connect(imap, smtp, email, pass);
            irABandeja();

        } catch (Exception e) {
            lblEstado.setText("Error: Credenciales inválidas o servidor no responde.");
        }
    }

    private String sugerirHost(String prov) {
        return switch (prov) {
            case "Gmail" -> "imap.gmail.com";
            case "Outlook" -> "outlook.office365.com";
            case "Yahoo" -> "imap.mail.yahoo.com";
            default -> "";
        };
    }

    private void irABandeja() throws Exception {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        
        MainController ctrl = loader.getController();
        ctrl.setMailService(mailService);

        Stage stage = (Stage) btnLogin.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}