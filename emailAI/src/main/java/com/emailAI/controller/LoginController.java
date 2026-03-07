package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.dao.DAOCuentas.CuentaGuardada;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.MailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoginController {

    @FXML
    private TilePane accountsTilePane;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    @FXML
    private ToggleButton themeToggle;

    @FXML
    private Button newAccountButton;

    @FXML
    private ImageView logoImage;

    private final List<CuentaGuardada> cuentas = new ArrayList<>();
    private CuentaGuardada cuentaSeleccionada;
    private final MailService mailService = new MailService();

    @FXML
    private void initialize() {
        try {
            Image logo = new Image(AppFX.class.getResourceAsStream("/logo.png"));
            logoImage.setImage(logo);
        } catch (Exception ignored) {}

        statusLabel.setText("");
        passwordField.setText("");
        themeToggle.setSelected(false); // false = tema oscuro por defecto

        cargarCuentasDesdeBD();
        dibujarTarjetas();
    }

    private void cargarCuentasDesdeBD() {
        cuentas.clear();
        try {
            DAOCuentas dao = new DAOCuentas();
            cuentas.addAll(dao.listarCuentas());
        } catch (Exception e) {
            statusLabel.setText("Error cargando cuentas: " + e.getMessage());
        }
    }

    private void dibujarTarjetas() {
        accountsTilePane.getChildren().clear();

        for (CuentaGuardada cuenta : cuentas) {
            VBox card = crearTarjetaCuenta(cuenta);
            accountsTilePane.getChildren().add(card);
        }

        VBox cardNueva = crearTarjetaNuevaCuenta();
        accountsTilePane.getChildren().add(cardNueva);
    }

    private VBox crearTarjetaCuenta(CuentaGuardada cuenta) {
        VBox card = new VBox();
        card.getStyleClass().add("account-card");
        card.setSpacing(4);

        String emailDescifrado;
        try {
            emailDescifrado = UtilidadCifrado.descifrar(cuenta.emailCifrado);
        } catch (Exception e) {
            emailDescifrado = "[email]";
        }

        Label emailLabel = new Label(emailDescifrado);
        emailLabel.getStyleClass().add("account-email");

        Label servidorLabel = new Label(cuenta.servidorImap);
        servidorLabel.getStyleClass().add("account-server");

        card.getChildren().addAll(emailLabel, servidorLabel);

        String finalEmail = emailDescifrado;
        card.setOnMouseClicked(ev -> seleccionarCuenta(cuenta, card, finalEmail));

        return card;
    }

    private VBox crearTarjetaNuevaCuenta() {
        VBox card = new VBox();
        card.getStyleClass().add("account-card-new");
        card.setSpacing(4);

        Label masLabel = new Label("+ Añadir cuenta");
        masLabel.getStyleClass().add("account-new-label");

        card.getChildren().add(masLabel);

        card.setOnMouseClicked(ev -> onNewAccountClicked(null));

        return card;
    }

    private void seleccionarCuenta(CuentaGuardada cuenta, VBox cardSeleccionada, String emailDescifrado) {
        this.cuentaSeleccionada = cuenta;

        accountsTilePane.getChildren()
                .forEach(node -> node.getStyleClass().remove("account-card-selected"));

        cardSeleccionada.getStyleClass().add("account-card-selected");
        statusLabel.setText("Cuenta seleccionada: " + emailDescifrado);
        passwordField.requestFocus();
    }

    @FXML
    private void onLoginClicked(ActionEvent event) {
        statusLabel.setText("");

        if (cuentaSeleccionada == null) {
            statusLabel.setText("Selecciona una cuenta primero.");
            return;
        }

        String password = passwordField.getText();
        if (password == null || password.isBlank()) {
            statusLabel.setText("Introduce la contraseña.");
            return;
        }

        String email;
        try {
            email = UtilidadCifrado.descifrar(cuentaSeleccionada.emailCifrado);
        } catch (Exception ex) {
            statusLabel.setText("Error descifrando el email: " + ex.getMessage());
            return;
        }

        try {
            mailService.connect(
                    cuentaSeleccionada.servidorImap,
                    cuentaSeleccionada.servidorSmtp,
                    email,
                    password
            );
            irAVentanaPrincipal(event);
        } catch (Exception e) {
            e.printStackTrace(); // para ver errores de conexión en consola
            statusLabel.setText("Error al conectar: " + e.getMessage());
        }
    }

    private void irAVentanaPrincipal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/main-view.fxml"));
            Scene mainScene = new Scene(loader.load());

            aplicarTemaAScene(mainScene);

            MainController mainController = loader.getController();
            if (mainController != null) {
                try {
                    mainController.setMailService(mailService);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("eMailAI");
            stage.setScene(mainScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace(); // <-- aquí veremos el LoadException de main-view
            statusLabel.setText("Error al conectar: " + e.getMessage());
        }
    }

    @FXML
    private void onThemeToggle(ActionEvent event) {
        Scene scene = themeToggle.getScene();
        if (scene != null) {
            aplicarTemaAScene(scene);
        }
    }

    private void aplicarTemaAScene(Scene scene) {
        scene.getStylesheets().clear();

        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-basic.css").toExternalForm()
        );

        if (themeToggle.isSelected()) {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-light.css").toExternalForm()
            );
        } else {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-dark.css").toExternalForm()
            );
        }
    }

    @FXML
    private void onNewAccountClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/config-view.fxml"));
            Scene configScene = new Scene(loader.load());

            aplicarTemaAScene(configScene);

            Stage stage;
            if (event != null) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) newAccountButton.getScene().getWindow();
            }

            stage.setTitle("eMailAI - Nueva cuenta");
            stage.setScene(configScene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("No se pudo abrir Nueva cuenta: " + e.getMessage());
        }
    }
}
