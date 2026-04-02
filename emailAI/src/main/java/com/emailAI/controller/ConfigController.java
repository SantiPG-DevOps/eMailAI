package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.model.ThemePreset;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.ThemeCssResolver;
import com.emailAI.service.ThemeManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class ConfigController {

    @FXML private ComboBox<String> providerCombo;
    @FXML private TextField emailField;
    @FXML private PasswordField masterPassField;
    @FXML private TextField imapField;
    @FXML private TextField smtpField;
    @FXML private Label imapLabel;
    @FXML private Label smtpLabel;
    @FXML private Label statusLabel;

    @FXML private RadioButton rbTemaClaro;
    @FXML private RadioButton rbTemaOscuro;
    @FXML private ComboBox<ThemePreset> comboTema;

    @FXML private VBox boxTemaPersonalizado;
    @FXML private TextField txtColorBg;
    @FXML private TextField txtColorSurface;
    @FXML private TextField txtColorText;
    @FXML private TextField txtColorMuted;
    @FXML private TextField txtColorPrimary;
    @FXML private TextField txtColorBorder;
    @FXML private Label lblTemaPersonalizadoEstado;

    @FXML private ComboBox<String> comboCuentasExistentes;
    @FXML private Spinner<Integer> spinnerSyncMinutos;
    @FXML private ListView<String> configNavList;
    @FXML private StackPane configStack;

    @FXML private TextField txtOllamaUrl;
    @FXML private TextField txtOllamaModelo;
    @FXML private Label lblTestIA;

    @FXML private Button btnResetOpciones;
    @FXML private Button saveButton;

    private static final String PREF_OLLAMA_URL = "ollama_url";
    private static final String PREF_OLLAMA_MODEL = "ollama_model";
    private static final String PREF_SYNC_INTERVAL_MINUTES = "sync_interval_minutes";
    private static final String PERSONALIZADO = "Servidor personalizado";

    private static final String PREF_CUSTOM_BG = "custom.bg";
    private static final String PREF_CUSTOM_SURFACE = "custom.surface";
    private static final String PREF_CUSTOM_TEXT = "custom.text";
    private static final String PREF_CUSTOM_MUTED = "custom.muted";
    private static final String PREF_CUSTOM_PRIMARY = "custom.primary";
    private static final String PREF_CUSTOM_BORDER = "custom.border";

    private final Map<String, String> mapaEmailAEmailCifrado = new HashMap<>();
    private final Map<String, String[]> proveedores = new HashMap<>();

    private final ThemeManager themeManager = new ThemeManager();
    private final ThemeCssResolver themeCssResolver = new ThemeCssResolver();

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        sincronizarControlesTema();
    }

    @FXML
    private void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        proveedores.put("Gmail", new String[]{"imap.gmail.com", "smtp.gmail.com"});
        proveedores.put("Outlook/Hotmail", new String[]{"imap-mail.outlook.com", "smtp-mail.outlook.com"});
        proveedores.put("Yahoo", new String[]{"imap.mail.yahoo.com", "smtp.mail.yahoo.com"});
        proveedores.put("iCloud", new String[]{"imap.mail.me.com", "smtp.mail.me.com"});
        proveedores.put("GMX", new String[]{"imap.gmx.com", "mail.gmx.com"});
        proveedores.put("ProtonMail (Bridge)", new String[]{"127.0.0.1", "127.0.0.1"});
        proveedores.put("Zoho Mail", new String[]{"imap.zoho.com", "smtp.zoho.com"});

        if (providerCombo != null) {
            providerCombo.getItems().addAll(
                    "Gmail", "Outlook/Hotmail", "Yahoo", "iCloud",
                    "GMX", "ProtonMail (Bridge)", "Zoho Mail", PERSONALIZADO
            );
            providerCombo.getSelectionModel().selectFirst();
            aplicarProveedorSeleccionado();
        }

        configurarComboTema();
        cargarTemaPersonalizado();
        sincronizarControlesTema();
        actualizarVisibilidadTemaPersonalizado();

        cargarCuentasExistentes();
        configurarSpinnerSincronizacion();
        configurarNavegacionAjustes();
        cargarConfigIA();
    }

    private void configurarComboTema() {
        if (comboTema == null) {
            return;
        }

        comboTema.setItems(FXCollections.observableArrayList(ThemePreset.values()));
        comboTema.setValue(themeManager.getTheme());

        comboTema.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ThemePreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatearTema(item));
            }
        });

        comboTema.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ThemePreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatearTema(item));
            }
        });
    }

    private void sincronizarControlesTema() {
        boolean light = "light".equalsIgnoreCase(themeManager.getMode());

        if (rbTemaClaro != null) {
            rbTemaClaro.setSelected(light);
        }
        if (rbTemaOscuro != null) {
            rbTemaOscuro.setSelected(!light);
        }
        if (comboTema != null) {
            comboTema.setValue(themeManager.getTheme());
        }
    }

    private void actualizarVisibilidadTemaPersonalizado() {
        boolean visible = comboTema != null && comboTema.getValue() == ThemePreset.PERSONALIZAR;

        if (boxTemaPersonalizado != null) {
            boxTemaPersonalizado.setVisible(visible);
            boxTemaPersonalizado.setManaged(visible);
        }
    }

    private void cargarTemaPersonalizado() {
        Preferences prefs = Preferences.userNodeForPackage(AppFX.class);

        if (txtColorBg != null) {
            txtColorBg.setText(prefs.get(PREF_CUSTOM_BG, "#0F172A"));
        }
        if (txtColorSurface != null) {
            txtColorSurface.setText(prefs.get(PREF_CUSTOM_SURFACE, "#1E293B"));
        }
        if (txtColorText != null) {
            txtColorText.setText(prefs.get(PREF_CUSTOM_TEXT, "#F1F5F9"));
        }
        if (txtColorMuted != null) {
            txtColorMuted.setText(prefs.get(PREF_CUSTOM_MUTED, "#94A3B8"));
        }
        if (txtColorPrimary != null) {
            txtColorPrimary.setText(prefs.get(PREF_CUSTOM_PRIMARY, "#22D3EE"));
        }
        if (txtColorBorder != null) {
            txtColorBorder.setText(prefs.get(PREF_CUSTOM_BORDER, "#334155"));
        }
    }

    private boolean esColorHexValido(String valor) {
        return valor != null && valor.matches("^#[0-9A-Fa-f]{6}$");
    }

    private void cargarConfigIA() {
        Preferences prefs = Preferences.userRoot().node("com/emailAI");
        if (txtOllamaUrl != null) {
            txtOllamaUrl.setText(prefs.get(PREF_OLLAMA_URL, "http://localhost:11434"));
        }
        if (txtOllamaModelo != null) {
            txtOllamaModelo.setText(prefs.get(PREF_OLLAMA_MODEL, "llama3"));
        }
    }

    @FXML
    private void onGuardarConfigIA() {
        String url = txtOllamaUrl != null ? txtOllamaUrl.getText().trim() : "";
        String model = txtOllamaModelo != null ? txtOllamaModelo.getText().trim() : "";

        if (url.isBlank() || model.isBlank()) {
            if (lblTestIA != null) {
                lblTestIA.setText("Rellena URL y modelo.");
            }
            return;
        }

        Preferences prefs = Preferences.userRoot().node("com/emailAI");
        prefs.put(PREF_OLLAMA_URL, url);
        prefs.put(PREF_OLLAMA_MODEL, model);

        if (lblTestIA != null) {
            lblTestIA.setText("Configuración guardada.");
        }
    }

    @FXML
    private void onTestOllama() {
        String url = txtOllamaUrl != null ? txtOllamaUrl.getText().trim() : "";
        if (url.isBlank()) {
            if (lblTestIA != null) {
                lblTestIA.setText("Introduce la URL primero.");
            }
            return;
        }

        if (lblTestIA != null) {
            lblTestIA.setText("Probando conexión…");
        }

        Thread t = new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        URI.create(url + "/api/tags").toURL().openConnection();
                conn.setConnectTimeout(5_000);
                conn.setReadTimeout(5_000);
                int code = conn.getResponseCode();

                String msg = code == 200
                        ? "✓ Ollama responde correctamente."
                        : "⚠ HTTP " + code;

                javafx.application.Platform.runLater(() -> {
                    if (lblTestIA != null) {
                        lblTestIA.setText(msg);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (lblTestIA != null) {
                        lblTestIA.setText("✗ Sin conexión: " + e.getMessage());
                    }
                });
            }
        }, "test-ollama");

        t.setDaemon(true);
        t.start();
    }

    private void configurarSpinnerSincronizacion() {
        if (spinnerSyncMinutos == null) {
            return;
        }

        int guardado = Math.max(5, Math.min(120,
                Preferences.userNodeForPackage(AppFX.class)
                        .getInt(PREF_SYNC_INTERVAL_MINUTES, 15)));

        spinnerSyncMinutos.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, guardado, 1)
        );
        spinnerSyncMinutos.setEditable(true);
    }

    @FXML
    private void onAplicarIntervaloSync(ActionEvent event) {
        if (spinnerSyncMinutos == null || spinnerSyncMinutos.getValue() == null) {
            return;
        }

        int m = Math.max(5, Math.min(120, spinnerSyncMinutos.getValue()));
        spinnerSyncMinutos.getValueFactory().setValue(m);
        Preferences.userNodeForPackage(AppFX.class).putInt(PREF_SYNC_INTERVAL_MINUTES, m);

        if (mainController != null) {
            mainController.reprogramarSincronizacionCorreo();
        }

        if (statusLabel != null) {
            statusLabel.setText("Intervalo guardado: cada " + m + " min.");
        }
    }

    private void configurarNavegacionAjustes() {
        if (configNavList == null || configStack == null) {
            return;
        }

        configNavList.getItems().addAll(
                "General", "Correo", "Calendario y tareas", "Contactos", "IA"
        );
        configNavList.setFixedCellSize(44);

        configNavList.getSelectionModel().selectedIndexProperty().addListener((obs, anterior, idx) -> {
            if (idx != null && idx.intValue() >= 0) {
                mostrarPaginaConfig(idx.intValue());
            }
        });

        configNavList.getSelectionModel().select(0);
    }

    private void mostrarPaginaConfig(int indice) {
        if (configStack == null) {
            return;
        }

        var hijos = configStack.getChildren();
        for (int i = 0; i < hijos.size(); i++) {
            boolean activa = (i == indice);
            hijos.get(i).setVisible(activa);
            hijos.get(i).setManaged(activa);
        }

        actualizarBotonesFooter(indice);
    }

    private void actualizarBotonesFooter(int indice) {
        boolean esGeneral = indice == 0;
        boolean esCorreo = indice == 1;

        if (btnResetOpciones != null) {
            btnResetOpciones.setVisible(esGeneral);
            btnResetOpciones.setManaged(esGeneral);
        }

        if (saveButton != null) {
            saveButton.setVisible(esCorreo);
            saveButton.setManaged(esCorreo);
        }
    }

    @FXML
    private void onProviderChanged(ActionEvent event) {
        aplicarProveedorSeleccionado();
    }

    private void aplicarProveedorSeleccionado() {
        if (providerCombo == null) {
            return;
        }

        String proveedor = providerCombo.getSelectionModel().getSelectedItem();
        if (proveedor == null) {
            return;
        }

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
            if (datos == null) {
                return;
            }

            if (imapField != null) {
                imapField.setText(datos[0]);
                imapField.setVisible(false);
            }
            if (imapLabel != null) imapLabel.setVisible(false);

            if (smtpField != null) {
                smtpField.setText(datos[1]);
                smtpField.setVisible(false);
            }
            if (smtpLabel != null) smtpLabel.setVisible(false);
        }
    }

    @FXML
    private void onSaveClicked(ActionEvent event) {
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        String proveedor = providerCombo != null ? providerCombo.getSelectionModel().getSelectedItem() : null;
        String email = emailField != null ? emailField.getText() : null;
        String masterPass = masterPassField != null ? masterPassField.getText() : null;

        if (proveedor == null) {
            if (statusLabel != null) statusLabel.setText("Selecciona un proveedor.");
            return;
        }

        String imap;
        String smtp;

        if (PERSONALIZADO.equals(proveedor)) {
            imap = imapField != null ? imapField.getText() : null;
            smtp = smtpField != null ? smtpField.getText() : null;
        } else {
            String[] datos = proveedores.get(proveedor);
            if (datos == null) {
                if (statusLabel != null) statusLabel.setText("Proveedor no válido.");
                return;
            }
            imap = datos[0];
            smtp = datos[1];
        }

        if (email == null || email.isBlank()
                || masterPass == null || masterPass.isBlank()
                || imap == null || imap.isBlank()
                || smtp == null || smtp.isBlank()) {
            if (statusLabel != null) statusLabel.setText("Rellena todos los campos.");
            return;
        }

        try {
            String emailCifrado = UtilidadCifrado.cifrar(email.trim());
            String passHash = UtilidadCifrado.hash(masterPass);

            DAOCuentas dao = new DAOCuentas();
            dao.guardar(new DAOCuentas.CuentaGuardada(
                    emailCifrado, imap, 993, smtp, passHash, false
            ));

            if (statusLabel != null) {
                statusLabel.setText("Cuenta guardada correctamente.");
            }

            cargarCuentasExistentes();
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Error guardando cuenta: " + e.getMessage());
            }
        }
    }

    private void cargarCuentasExistentes() {
        if (comboCuentasExistentes == null) {
            return;
        }

        try {
            DAOCuentas dao = new DAOCuentas();
            comboCuentasExistentes.getItems().clear();
            mapaEmailAEmailCifrado.clear();

            for (DAOCuentas.CuentaGuardada c : dao.listarTodas()) {
                String emailPlano = UtilidadCifrado.descifrar(c.email());
                comboCuentasExistentes.getItems().add(emailPlano);
                mapaEmailAEmailCifrado.put(emailPlano, c.email());
            }
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Error cargando cuentas: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onOlvidarCuentaClicked(ActionEvent event) {
        if (comboCuentasExistentes == null) {
            return;
        }

        String seleccion = comboCuentasExistentes.getSelectionModel().getSelectedItem();
        if (seleccion == null || seleccion.isBlank()) {
            if (statusLabel != null) statusLabel.setText("Selecciona una cuenta para olvidar.");
            return;
        }

        String emailCifrado = mapaEmailAEmailCifrado.get(seleccion);
        if (emailCifrado == null) {
            if (statusLabel != null) statusLabel.setText("Cuenta no encontrada.");
            return;
        }

        try {
            new DAOCuentas().eliminar(emailCifrado);
            if (statusLabel != null) {
                statusLabel.setText("Cuenta olvidada correctamente.");
            }
            cargarCuentasExistentes();
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Error al olvidar cuenta: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onTemaClaro() {
        themeManager.setMode("light");
        sincronizarControlesTema();
        aplicarTemaActual();
    }

    @FXML
    private void onTemaOscuro() {
        themeManager.setMode("dark");
        sincronizarControlesTema();
        aplicarTemaActual();
    }

    @FXML
    private void onTemaSeleccionado() {
        if (comboTema == null || comboTema.getValue() == null) {
            return;
        }

        themeManager.setTheme(comboTema.getValue());
        actualizarVisibilidadTemaPersonalizado();
        aplicarTemaActual();
    }

    @FXML
    private void onAplicarTemaPersonalizado() {
        String bg = txtColorBg != null ? txtColorBg.getText().trim() : "";
        String surface = txtColorSurface != null ? txtColorSurface.getText().trim() : "";
        String text = txtColorText != null ? txtColorText.getText().trim() : "";
        String muted = txtColorMuted != null ? txtColorMuted.getText().trim() : "";
        String primary = txtColorPrimary != null ? txtColorPrimary.getText().trim() : "";
        String border = txtColorBorder != null ? txtColorBorder.getText().trim() : "";

        if (!esColorHexValido(bg)
                || !esColorHexValido(surface)
                || !esColorHexValido(text)
                || !esColorHexValido(muted)
                || !esColorHexValido(primary)
                || !esColorHexValido(border)) {

            if (lblTemaPersonalizadoEstado != null) {
                lblTemaPersonalizadoEstado.setText("Revisa los colores. Usa formato #RRGGBB.");
            }
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(AppFX.class);
        prefs.put(PREF_CUSTOM_BG, bg);
        prefs.put(PREF_CUSTOM_SURFACE, surface);
        prefs.put(PREF_CUSTOM_TEXT, text);
        prefs.put(PREF_CUSTOM_MUTED, muted);
        prefs.put(PREF_CUSTOM_PRIMARY, primary);
        prefs.put(PREF_CUSTOM_BORDER, border);

        themeManager.setTheme(ThemePreset.PERSONALIZAR);
        if (comboTema != null) {
            comboTema.setValue(ThemePreset.PERSONALIZAR);
        }

        if (lblTemaPersonalizadoEstado != null) {
            lblTemaPersonalizadoEstado.setText("Colores personalizados guardados.");
        }

        actualizarVisibilidadTemaPersonalizado();
        aplicarTemaActual();
    }

    @FXML
    private void onResetOpcionesClicked() {
        themeManager.setTheme(ThemePreset.EMAILIA);
        themeManager.setMode("dark");

        Preferences prefs = Preferences.userNodeForPackage(AppFX.class);
        prefs.remove(PREF_CUSTOM_BG);
        prefs.remove(PREF_CUSTOM_SURFACE);
        prefs.remove(PREF_CUSTOM_TEXT);
        prefs.remove(PREF_CUSTOM_MUTED);
        prefs.remove(PREF_CUSTOM_PRIMARY);
        prefs.remove(PREF_CUSTOM_BORDER);

        cargarTemaPersonalizado();
        sincronizarControlesTema();
        actualizarVisibilidadTemaPersonalizado();
        aplicarTemaActual();

        if (lblTemaPersonalizadoEstado != null) {
            lblTemaPersonalizadoEstado.setText("");
        }
        if (statusLabel != null) {
            statusLabel.setText("Apariencia restaurada a EmailIA oscuro.");
        }
    }

    private void aplicarTemaActual() {
        boolean light = "light".equalsIgnoreCase(themeManager.getMode());

        if (mainController != null) {
            mainController.aplicarTema(light);
        } else {
            aplicarTemaLocal(light);
        }
    }

    private void aplicarTemaLocal(boolean light) {
        Scene scene = null;

        if (comboTema != null && comboTema.getScene() != null) {
            scene = comboTema.getScene();
        } else if (rbTemaClaro != null) {
            scene = rbTemaClaro.getScene();
        }

        if (scene == null) {
            return;
        }

        var basicUrl = AppFX.class.getResource("/styles-basic.css");
        String themePath = themeCssResolver.getCssPath(themeManager.getTheme(), light ? "light" : "dark");
        var themeUrl = AppFX.class.getResource(themePath);

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
    private void onCancelClicked(ActionEvent event) {
        try {
            irALogin(event);
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Error al volver al login: " + e.getMessage());
            }
        }
    }

    private void irALogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/login-view.fxml"));
        Scene loginScene = new Scene(loader.load());

        var basicUrl = AppFX.class.getResource("/styles-basic.css");
        String themePath = themeCssResolver.getCssPath(themeManager.getTheme(), themeManager.getMode());
        var themeUrl = AppFX.class.getResource(themePath);

        if (basicUrl == null) {
            throw new IllegalStateException("No se encontró /styles-basic.css");
        }
        if (themeUrl == null) {
            throw new IllegalStateException("No se encontró el CSS del tema: " + themePath);
        }

        loginScene.getStylesheets().add(basicUrl.toExternalForm());
        loginScene.getStylesheets().add(themeUrl.toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("eMailAI - Login");
        stage.setScene(loginScene);
        stage.show();
    }

    private String formatearTema(ThemePreset tema) {
        return switch (tema) {
            case EMAILIA -> "EmailIA";
            case OCEAN_TEAL -> "Ocean Teal";
            case GRAPHITE_MINT -> "Graphite Mint";
            case AMBER_SLATE -> "Amber Slate";
            case INDIGO_MIST -> "Indigo Mist";
            case EMERALD_PAPER -> "Emerald Paper";
            case RUBY_INK -> "Ruby Ink";
            case VIOLET_SMOKE -> "Violet Smoke";
            case SLATE_GOLD -> "Slate Gold";
            case ARCTIC_BLUE -> "Arctic Blue";
            case SAND_CORAL -> "Sand Coral";
            case FOREST_STONE -> "Forest Stone";
            case MIDNIGHT_ROSE -> "Midnight Rose";
            case NEUTRAL_CYAN -> "Neutral Cyan";
            case WARM_BEIGE_TEAL -> "Warm Beige Teal";
            case DEEP_NAVY_LIME -> "Deep Navy Lime";
            case PERSONALIZAR -> "Personalizar";
        };
    }
}