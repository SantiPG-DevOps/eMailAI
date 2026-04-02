package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.dao.DAOCuentas.CuentaGuardada;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.MailService;
import com.emailAI.service.ThemeCssResolver;
import com.emailAI.service.ThemeManager;
import com.emailAI.model.ThemePreset;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LoginController {

    @FXML private VBox accountsList;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private ToggleButton themeToggle;
    @FXML private Button newAccountButton;
    @FXML private ImageView logoImage;
    @FXML private ProgressBar strengthBar;
    @FXML private Label strengthLabel;

    private final List<CuentaGuardada> cuentas = new ArrayList<>();
    private CuentaGuardada cuentaSeleccionada;
    private final MailService mailService = new MailService();
    private boolean temaClaro = false;

    private final ThemeManager themeManager = new ThemeManager();
    private final ThemeCssResolver themeCssResolver = new ThemeCssResolver();

    @FXML
    private void initialize() {
        try {
            Image logo = new Image(AppFX.class.getResourceAsStream("/logo.png"));
            logoImage.setImage(logo);
        } catch (Exception ignored) {}

        statusLabel.setText("");
        passwordField.setText("");

        temaClaro = "light".equalsIgnoreCase(themeManager.getMode());
        if (themeToggle != null) {
            themeToggle.setSelected(temaClaro);
            themeToggle.setText(temaClaro ? "☼" : "☾");
        }

        if (strengthBar != null && strengthLabel != null) {
            strengthBar.setProgress(0.0);
            strengthLabel.setText("Contraseña débil");
        }

        cargarCuentasDesdeBD();
        dibujarTarjetas();

        Scene scene = themeToggle != null ? themeToggle.getScene() : null;
        if (scene != null) {
            aplicarTemaAScene(scene, temaClaro);
        }
    }

    private void cargarCuentasDesdeBD() {
        cuentas.clear();
        try {
            DAOCuentas dao = new DAOCuentas();
            cuentas.addAll(dao.listarTodas());
        } catch (Exception e) {
            statusLabel.setText("Error cargando cuentas: " + e.getMessage());
        }
    }

    private void dibujarTarjetas() {
        if (accountsList == null) {
            return;
        }
        accountsList.getChildren().clear();
        for (CuentaGuardada cuenta : cuentas) {
            accountsList.getChildren().add(crearTarjetaCuenta(cuenta));
        }
    }

    private VBox crearTarjetaCuenta(CuentaGuardada cuenta) {
        VBox card = new VBox();
        card.getStyleClass().add("account-card");
        card.setSpacing(4);
        card.setMaxWidth(560);

        String emailDescifrado;
        try {
            emailDescifrado = UtilidadCifrado.descifrar(cuenta.email());
        } catch (Exception e) {
            emailDescifrado = "[email]";
        }

        Label emailLabel = new Label(emailDescifrado);
        emailLabel.getStyleClass().add("account-email");

        Label servidorLabel = new Label(cuenta.servidor());
        servidorLabel.getStyleClass().add("account-server");

        card.getChildren().addAll(emailLabel, servidorLabel);

        String finalEmail = emailDescifrado;
        card.setOnMouseClicked(ev -> seleccionarCuenta(cuenta, card, finalEmail));

        return card;
    }

    private void seleccionarCuenta(CuentaGuardada cuenta, VBox cardSeleccionada, String emailDescifrado) {
        this.cuentaSeleccionada = cuenta;
        accountsList.getChildren().forEach(node -> node.getStyleClass().remove("account-card-selected"));
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
            email = UtilidadCifrado.descifrar(cuentaSeleccionada.email());
        } catch (Exception ex) {
            statusLabel.setText("Error descifrando el email: " + ex.getMessage());
            return;
        }

        try {
            mailService.connect(
                    cuentaSeleccionada.servidor(),
                    cuentaSeleccionada.usuarioCifrado(),
                    email,
                    password
            );
            irAVentanaPrincipal(event);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error al conectar: " + e.getMessage());
        }
    }

    private void irAVentanaPrincipal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/main-view.fxml"));
            Scene mainScene = new Scene(loader.load());

            aplicarTemaAScene(mainScene, temaClaro);

            MainController mainController = loader.getController();
            if (mainController != null) {
                AppFX.setMainController(mainController);
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

    @FXML
    private void onThemeToggle(ActionEvent event) {
        if (themeToggle == null) {
            return;
        }
        temaClaro = themeToggle.isSelected();
        themeToggle.setText(temaClaro ? "☼" : "☾");

        Scene scene = themeToggle.getScene();
        if (scene != null) {
            aplicarTemaAScene(scene, temaClaro);
        }
    }

    private void aplicarTemaAScene(Scene scene, boolean light) {
        if (scene == null) {
            return;
        }

        themeManager.setMode(light ? "light" : "dark");
        ThemePreset tema = themeManager.getTheme();

        var basicUrl = getClass().getResource("/styles-basic.css");
        String themePath = themeCssResolver.getCssPath(tema, themeManager.getMode());
        var themeUrl = getClass().getResource(themePath);

        if (basicUrl == null) {
            throw new IllegalStateException("No se encontró /styles-basic.css");
        }
        if (themeUrl == null) {
            throw new IllegalStateException("No se encontró el CSS del tema: " + themePath);
        }

        scene.getStylesheets().clear();
        scene.getStylesheets().add(basicUrl.toExternalForm());
        scene.getStylesheets().add(themeUrl.toExternalForm());
    }

    @FXML
    private void onNewAccountClicked(ActionEvent event) {
        statusLabel.setText("");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nueva cuenta");
        dialog.setHeaderText("Añadir cuenta de correo");

        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        if (owner.getScene() != null) {
            pane.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        Map<String, String[]> proveedores = new HashMap<>();
        proveedores.put("Gmail", new String[]{"imap.gmail.com", "smtp.gmail.com"});
        proveedores.put("Outlook/Hotmail", new String[]{"imap-mail.outlook.com", "smtp-mail.outlook.com"});
        proveedores.put("Yahoo", new String[]{"imap.mail.yahoo.com", "smtp.mail.yahoo.com"});
        proveedores.put("iCloud", new String[]{"imap.mail.me.com", "smtp.mail.me.com"});
        proveedores.put("GMX", new String[]{"imap.gmx.com", "mail.gmx.com"});
        proveedores.put("ProtonMail (Bridge)", new String[]{"127.0.0.1", "127.0.0.1"});
        proveedores.put("Zoho Mail", new String[]{"imap.zoho.com", "smtp.zoho.com"});
        String personalizado = "Servidor personalizado";

        ComboBox<String> providerCombo = new ComboBox<>();
        providerCombo.getItems().addAll(
                "Gmail", "Outlook/Hotmail", "Yahoo", "iCloud",
                "GMX", "ProtonMail (Bridge)", "Zoho Mail", personalizado
        );
        providerCombo.getSelectionModel().selectFirst();

        TextField emailField = new TextField();
        PasswordField masterPassField = new PasswordField();
        emailField.setPromptText("usuario@dominio.com");
        masterPassField.setPromptText("Contraseña maestra");

        Label imapLabel = new Label("Servidor IMAP:");
        TextField imapField = new TextField();
        Label smtpLabel = new Label("Servidor SMTP:");
        TextField smtpField = new TextField();

        Runnable aplicarProveedor = () -> {
            String p = providerCombo.getSelectionModel().getSelectedItem();
            if (p == null) {
                return;
            }
            if (personalizado.equals(p)) {
                imapLabel.setVisible(true);
                smtpLabel.setVisible(true);
                imapField.setVisible(true);
                smtpField.setVisible(true);
                imapField.setEditable(true);
                smtpField.setEditable(true);
                imapField.clear();
                smtpField.clear();
            } else {
                String[] datos = proveedores.get(p);
                if (datos != null) {
                    imapField.setText(datos[0]);
                    smtpField.setText(datos[1]);
                }
                imapLabel.setVisible(false);
                smtpLabel.setVisible(false);
                imapField.setVisible(false);
                smtpField.setVisible(false);
            }
        };

        providerCombo.setOnAction(e -> aplicarProveedor.run());
        aplicarProveedor.run();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Proveedor:"), 0, 0);
        grid.add(providerCombo, 1, 0);
        grid.add(new Label("Correo electrónico:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Contraseña maestra:"), 0, 2);
        grid.add(masterPassField, 1, 2);
        grid.add(imapLabel, 0, 3);
        grid.add(imapField, 1, 3);
        grid.add(smtpLabel, 0, 4);
        grid.add(smtpField, 1, 4);
        pane.setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String proveedor = providerCombo.getSelectionModel().getSelectedItem();
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String passMaestra = masterPassField.getText() != null ? masterPassField.getText() : "";

        String imap = "";
        String smtp = "";
        if (personalizado.equals(proveedor)) {
            imap = imapField.getText() != null ? imapField.getText().trim() : "";
            smtp = smtpField.getText() != null ? smtpField.getText().trim() : "";
        } else if (proveedor != null && proveedores.containsKey(proveedor)) {
            imap = proveedores.get(proveedor)[0];
            smtp = proveedores.get(proveedor)[1];
        }

        if (email.isBlank() || passMaestra.isBlank() || imap.isBlank() || smtp.isBlank()) {
            statusLabel.setText("Cuenta no creada: completa todos los campos.");
            return;
        }

        try {
            String emailCifrado = UtilidadCifrado.cifrar(email);
            String passHash = UtilidadCifrado.hash(passMaestra);

            DAOCuentas dao = new DAOCuentas();
            dao.guardar(new CuentaGuardada(
                    emailCifrado,
                    imap,
                    993,
                    smtp,
                    passHash,
                    false
            ));

            cargarCuentasDesdeBD();
            dibujarTarjetas();
            statusLabel.setText("Cuenta creada correctamente.");
        } catch (Exception e) {
            statusLabel.setText("Error al crear la cuenta: " + e.getMessage());
        }
    }

    @FXML
    private void onAppPasswordChanged() {
        if (strengthBar == null || strengthLabel == null) {
            return;
        }
        actualizarStrength(passwordField.getText());
    }

    private void actualizarStrength(String pwd) {
        int nivel = calcularNivel(pwd);
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

    private int calcularNivel(String pwd) {
        if (pwd == null || pwd.isBlank()) {
            return 0;
        }
        boolean tieneMinus = pwd.matches(".*[a-z].*");
        boolean tieneMayus = pwd.matches(".*[A-Z].*");
        boolean tieneNum = pwd.matches(".*\\d.*");
        boolean tieneEsp = pwd.matches(".*[^A-Za-z0-9].*");
        int longitud = pwd.length();
        int puntos = 0;
        if (tieneMinus) puntos++;
        if (tieneMayus) puntos++;
        if (tieneNum) puntos++;
        if (tieneEsp) puntos++;
        if (longitud >= 12) puntos++;
        else if (longitud >= 8) puntos++;
        if (puntos <= 2) return 0;
        else if (puntos <= 4) return 1;
        else return 2;
    }
}