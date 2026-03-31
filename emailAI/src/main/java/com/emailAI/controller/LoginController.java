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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Gestiona la pantalla de login, selección de cuentas guardadas y cambio de tema inicial.
public class LoginController {

    // Contenedor donde se pintan las tarjetas de cuentas disponibles.
    @FXML
    private VBox accountsList;

    // Campo donde el usuario introduce la contraseña de la cuenta seleccionada.
    @FXML
    private PasswordField passwordField;

    // Mensajes de error/estado durante el proceso de login.
    @FXML
    private Label statusLabel;

    // Botón para alternar entre tema claro y oscuro en la pantalla de login.
    @FXML
    private ToggleButton themeToggle;

    // Botón para ir a la pantalla de configuración de nueva cuenta.
    @FXML
    private Button newAccountButton;

    // Logo mostrado en la cabecera del login.
    @FXML
    private ImageView logoImage;

    // Barra visual que indica la fuerza de la contraseña introducida.
    @FXML
    private ProgressBar strengthBar;

    // Texto descriptivo asociado al nivel de fuerza de contraseña.
    @FXML
    private Label strengthLabel;

    private final List<CuentaGuardada> cuentas = new ArrayList<>(); // Lista de cuentas cargadas desde BD.
    private CuentaGuardada cuentaSeleccionada; // Cuenta actualmente resaltada en la UI.
    private final MailService mailService = new MailService(); // Servicio de correo usado tras el login.

    // estado de tema en login (true = claro, false = oscuro)
    private boolean temaClaro = false;

    // Inicializa el login cargando logo, tema por defecto y cuentas guardadas.
    @FXML
    private void initialize() {
        try {
            Image logo = new Image(AppFX.class.getResourceAsStream("/logo.png"));
            logoImage.setImage(logo);
        } catch (Exception ignored) {}

        statusLabel.setText("");
        passwordField.setText("");

        // Tema por defecto: oscuro
        if (themeToggle != null) {
            temaClaro = false;
            themeToggle.setSelected(false);
            themeToggle.setText("☾");
        }

        if (strengthBar != null && strengthLabel != null) {
            strengthBar.setProgress(0.0);
            strengthLabel.setText("Contraseña débil");
        }

        cargarCuentasDesdeBD();
        dibujarTarjetas();
    }

    // ===================== Cuentas guardadas =====================

    // Carga la lista de cuentas guardadas desde la base de datos.
    private void cargarCuentasDesdeBD() {
        cuentas.clear();
        try {
            DAOCuentas dao = new DAOCuentas();
            cuentas.addAll(dao.listarCuentas());
        } catch (Exception e) {
            statusLabel.setText("Error cargando cuentas: " + e.getMessage());
        }
    }

    // Dibuja una tarjeta visual por cada cuenta disponible en el VBox.
    private void dibujarTarjetas() {
        accountsList.getChildren().clear();

        for (CuentaGuardada cuenta : cuentas) {
            VBox card = crearTarjetaCuenta(cuenta);
            accountsList.getChildren().add(card);
        }
    }

    // Crea el nodo visual que representa una cuenta en la lista.
    private VBox crearTarjetaCuenta(CuentaGuardada cuenta) {
        VBox card = new VBox();
        card.getStyleClass().add("account-card");
        card.setSpacing(4);
        card.setMaxWidth(560);

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

    // Marca una cuenta como seleccionada y actualiza el estado visual y de texto.
    private void seleccionarCuenta(CuentaGuardada cuenta,
                                   VBox cardSeleccionada,
                                   String emailDescifrado) {
        this.cuentaSeleccionada = cuenta;

        accountsList.getChildren()
                .forEach(node -> node.getStyleClass().remove("account-card-selected"));

        cardSeleccionada.getStyleClass().add("account-card-selected");

        statusLabel.setText("Cuenta seleccionada: " + emailDescifrado);
        passwordField.requestFocus();
    }

    // ===================== Login =====================

    // Maneja el click en el botón de login: valida, descifra email y conecta con el servidor.
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
            e.printStackTrace();
            statusLabel.setText("Error al conectar: " + e.getMessage());
        }
    }

    // Carga la ventana principal y le pasa el MailService y el tema elegido.
    private void irAVentanaPrincipal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/main-view.fxml"));
            Scene mainScene = new Scene(loader.load());

            // Aplica mismo tema que había en login
            aplicarTemaAScene(mainScene);

            MainController mainController = loader.getController();
            if (mainController != null) {
                // Registrar mainController globalmente para que CorreoController/compose puedan leer el tema
                AppFX.setMainController(mainController);

                mainController.aplicarTema(temaClaro);
                mainController.setMailService(mailService);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("eMailAI");
            stage.setScene(mainScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error al abrir la ventana principal: " + e.getMessage());
        }
    }

    // ===================== Tema claro/oscuro =====================

    // Alterna el tema en caliente dentro del login y actualiza estilos de la escena.
    @FXML
    private void onThemeToggle(ActionEvent event) {
        if (themeToggle == null) return;

        temaClaro = themeToggle.isSelected();
        themeToggle.setText(temaClaro ? "☼" : "☾");

        Scene scene = themeToggle.getScene();
        if (scene != null) {
            aplicarTemaAScene(scene);
        }
    }

    // Aplica el conjunto de hojas de estilo según tema claro/oscuro elegido.
    private void aplicarTemaAScene(Scene scene) {
        scene.getStylesheets().clear();

        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-basic.css").toExternalForm()
        );

        if (temaClaro) {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-light.css").toExternalForm()
            );
        } else {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-dark.css").toExternalForm()
            );
        }
    }

    // ===================== Nueva cuenta =====================

    // Abre la pantalla de configuración para crear una nueva cuenta de correo.
    @FXML
    private void onNewAccountClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/config-view.fxml"));
            Scene configScene = new Scene(loader.load());

            aplicarTemaAScene(configScene);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("eMailAI - Nueva cuenta");
            stage.setScene(configScene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("No se pudo abrir Nueva cuenta: " + e.getMessage());
        }
    }

    // ===================== Fuerza de contraseña =====================

    // Evento que se dispara al cambiar el texto de la contraseña de app.
    @FXML
    private void onAppPasswordChanged() {
        if (strengthBar == null || strengthLabel == null) return;
        String pwd = passwordField.getText();
        actualizarStrength(pwd);
    }

    // Actualiza barra y texto de fuerza de contraseña a partir del nivel calculado.
    private void actualizarStrength(String pwd) {
        int nivel = calcularNivel(pwd); // 0 débil, 1 media, 2 fuerte
        double progress;
        String texto;

        switch (nivel) {
            case 0 -> {
                progress = 0.2;
                texto = "Contraseña débil";
            }
            case 1 -> {
                progress = 0.6;
                texto = "Contraseña media";
            }
            default -> {
                progress = 1.0;
                texto = "Contraseña fuerte";
            }
        }
        strengthBar.setProgress(progress);
        strengthLabel.setText(texto);
    }

    // Calcula un nivel simple de fuerza de contraseña en función de composición y longitud.
    private int calcularNivel(String pwd) {
        if (pwd == null || pwd.isBlank()) return 0;

        boolean tieneMinus = pwd.matches(".*[a-z].*");
        boolean tieneMayus = pwd.matches(".*[A-Z].*");
        boolean tieneNum   = pwd.matches(".*\\d.*");
        boolean tieneEsp   = pwd.matches(".*[^A-Za-z0-9].*");
        int longitud = pwd.length();

        int puntos = 0;
        if (tieneMinus) puntos++;
        if (tieneMayus) puntos++;
        if (tieneNum)   puntos++;
        if (tieneEsp)   puntos++;
        if (longitud >= 12) puntos++;
        else if (longitud >= 8) puntos++;

        if (puntos <= 2) return 0;      // débil
        else if (puntos <= 4) return 1; // media
        else return 2;                  // fuerte
    }
}