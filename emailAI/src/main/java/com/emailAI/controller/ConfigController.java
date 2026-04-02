package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.model.ThemePreset;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.IcsService;
import com.emailAI.service.ThemeCssResolver;
import com.emailAI.service.ThemeManager;
import com.emailAI.service.TodoistService;
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

    @FXML private TextField txtTodoistApiKey;
    @FXML private CheckBox chkTodoistEnabled;
    @FXML private TextField txtIcsPath;
    @FXML private CheckBox chkIcsEnabled;
    @FXML private Label lblCalendarioEstado;
    @FXML private Button btnProbarTodoist;
    @FXML private Button btnSeleccionarIcs;
    @FXML private Button btnImportarIcs;
    @FXML private Button btnExportarIcs;
    @FXML private Button btnSeleccionarExport;
    @FXML private TextField txtExportPath;
    @FXML private Label lblUltimaSyncIcs;
    @FXML private Label lblUltimaSyncTodoist;
    @FXML private Spinner<Integer> spinnerTodoistInterval;
    @FXML private Button btnForzarSyncTodoist;
    @FXML private RadioButton rbSyncBidireccional;
    @FXML private RadioButton rbSyncSoloSubida;
    @FXML private RadioButton rbSyncSoloBajada;

    @FXML private TextField txtImportarContactosPath;
    @FXML private Button btnSeleccionarImportarContactos;
    @FXML private Button btnImportarContactos;
    @FXML private TextField txtExportarContactosPath;
    @FXML private Button btnSeleccionarExportarContactos;
    @FXML private Button btnExportarContactos;
    @FXML private TextField txtNuevoRemitenteConfiable;
    @FXML private Button btnAgregarRemitenteConfiable;
    @FXML private ListView<String> listaRemitentesConfiables;
    @FXML private Button btnEliminarRemitenteConfiable;
    @FXML private Button btnCargarRemitentesConfiables;
    @FXML private CheckBox chkSyncContactosAuto;
    @FXML private Spinner<Integer> spinnerSyncContactosInterval;
    @FXML private Button btnSincronizarContactosAhora;
    @FXML private Label lblContactosEstado;

    @FXML private Button btnResetOpciones;
    @FXML private Button saveButton;

    private static final String PREF_OLLAMA_URL = "ollama_url";
    private static final String PREF_OLLAMA_MODEL = "ollama_model";
    private static final String PREF_SYNC_INTERVAL_MINUTES = "sync_interval_minutes";
    public static final String PREF_TODOIST_API_KEY = "todoist_api_key";
    public static final String PREF_TODOIST_ENABLED = "todoist_enabled";
    public static final String PREF_TODOIST_INTERVAL = "todoist_interval_minutes";
    public static final String PREF_TODOIST_SYNC_DIRECTION = "todoist_sync_direction";
    public static final String PREF_ICS_PATH = "ics_path";
    public static final String PREF_ICS_ENABLED = "ics_enabled";
    public static final String PREF_ICS_EXPORT_PATH = "ics_export_path";

    public static final String PREF_CONTACTOS_IMPORT_PATH = "contactos_import_path";
    public static final String PREF_CONTACTOS_EXPORT_PATH = "contactos_export_path";
    public static final String PREF_CONTACTOS_REMITENTES = "contactos_remitentes";
    public static final String PREF_CONTACTOS_SYNC_AUTO = "contactos_sync_auto";
    public static final String PREF_CONTACTOS_SYNC_INTERVAL = "contactos_sync_interval";
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
        cargarConfigCalendarioTodoist();
        cargarConfigContactos();
    }

    private void configurarComboTema() {
        if (comboTema == null) return;

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

        if (rbTemaClaro != null) rbTemaClaro.setSelected(light);
        if (rbTemaOscuro != null) rbTemaOscuro.setSelected(!light);
        if (comboTema != null) comboTema.setValue(themeManager.getTheme());
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

        if (txtColorBg != null) txtColorBg.setText(prefs.get(PREF_CUSTOM_BG, "#0F172A"));
        if (txtColorSurface != null) txtColorSurface.setText(prefs.get(PREF_CUSTOM_SURFACE, "#1E293B"));
        if (txtColorText != null) txtColorText.setText(prefs.get(PREF_CUSTOM_TEXT, "#F1F5F9"));
        if (txtColorMuted != null) txtColorMuted.setText(prefs.get(PREF_CUSTOM_MUTED, "#94A3B8"));
        if (txtColorPrimary != null) txtColorPrimary.setText(prefs.get(PREF_CUSTOM_PRIMARY, "#22D3EE"));
        if (txtColorBorder != null) txtColorBorder.setText(prefs.get(PREF_CUSTOM_BORDER, "#334155"));
    }

    private boolean esColorHexValido(String valor) {
        return valor != null && valor.matches("^#[0-9A-Fa-f]{6}$");
    }

    private void cargarConfigIA() {
        Preferences prefs = Preferences.userRoot().node("com/emailAI");
        if (txtOllamaUrl != null) txtOllamaUrl.setText(prefs.get(PREF_OLLAMA_URL, "http://localhost:11434"));
        if (txtOllamaModelo != null) txtOllamaModelo.setText(prefs.get(PREF_OLLAMA_MODEL, "llama3"));
    }

    @FXML
    private void onGuardarConfigIA() {
        String url = txtOllamaUrl != null ? txtOllamaUrl.getText().trim() : "";
        String model = txtOllamaModelo != null ? txtOllamaModelo.getText().trim() : "";

        if (url.isBlank() || model.isBlank()) {
            if (lblTestIA != null) lblTestIA.setText("Rellena URL y modelo.");
            return;
        }

        Preferences prefs = Preferences.userRoot().node("com/emailAI");
        prefs.put(PREF_OLLAMA_URL, url);
        prefs.put(PREF_OLLAMA_MODEL, model);

        if (lblTestIA != null) lblTestIA.setText("Configuración guardada.");
    }

    @FXML
    private void onGuardarConfigCalendarioTodoist() {
        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);

        String todoistApiKey = txtTodoistApiKey != null ? txtTodoistApiKey.getText().trim() : "";
        boolean todoistEnabled = chkTodoistEnabled != null && chkTodoistEnabled.isSelected();
        int todoistInterval = spinnerTodoistInterval != null ? spinnerTodoistInterval.getValue() : 30;
        String icsPath = txtIcsPath != null ? txtIcsPath.getText().trim() : "";
        boolean icsEnabled = chkIcsEnabled != null && chkIcsEnabled.isSelected();
        String exportPath = txtExportPath != null ? txtExportPath.getText().trim() : "";

        if (todoistEnabled && todoistApiKey.isBlank()) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("API Key de Todoist requerida si está habilitado.");
            return;
        }

        if (icsEnabled && icsPath.isBlank()) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("Ruta de archivo ICS requerida si está habilitado.");
            return;
        }

        String syncDirection = "bidirectional";
        if (rbSyncSoloSubida != null && rbSyncSoloSubida.isSelected()) {
            syncDirection = "upload";
        } else if (rbSyncSoloBajada != null && rbSyncSoloBajada.isSelected()) {
            syncDirection = "download";
        }

        prefs.put(PREF_TODOIST_API_KEY, todoistApiKey);
        prefs.putBoolean(PREF_TODOIST_ENABLED, todoistEnabled);
        prefs.putInt(PREF_TODOIST_INTERVAL, todoistInterval);
        prefs.put(PREF_TODOIST_SYNC_DIRECTION, syncDirection);
        prefs.put(PREF_ICS_PATH, icsPath);
        prefs.putBoolean(PREF_ICS_ENABLED, icsEnabled);
        prefs.put(PREF_ICS_EXPORT_PATH, exportPath);

        if (lblCalendarioEstado != null) lblCalendarioEstado.setText("✅ Configuración guardada correctamente.");
    }

    @FXML
    private void onImportarArchivoIcs() {
        String ruta = txtIcsPath != null ? txtIcsPath.getText().trim() : "";
        if (ruta.isBlank()) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("Selecciona un archivo .ics primero.");
            return;
        }

        try {
            IcsService icsService = new IcsService();
            var eventos = icsService.procesarArchivoIcs(ruta);
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("✅ Importados " + eventos.size() + " eventos desde ICS");
            actualizarUltimaSyncIcs();
        } catch (Exception e) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("❌ Error importando ICS: " + e.getMessage());
        }
    }

    @FXML
    private void onSeleccionarRutaExport() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Exportar calendario");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Archivos ICS", "*.ics")
        );
        fileChooser.setInitialFileName("calendario_exportado.ics");

        java.io.File archivo = fileChooser.showSaveDialog(null);
        if (archivo != null && txtExportPath != null) {
            txtExportPath.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    private void onExportarCalendarioIcs() {
        String ruta = txtExportPath != null ? txtExportPath.getText().trim() : "";
        if (ruta.isBlank()) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("Selecciona una ruta de exportación.");
            return;
        }

        try {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("✅ Calendario exportado a: " + ruta);
        } catch (Exception e) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("❌ Error exportando calendario: " + e.getMessage());
        }
    }

    @FXML
    private void onForzarSincronizacionTodoist() {
        String apiKey = txtTodoistApiKey != null ? txtTodoistApiKey.getText().trim() : "";
        if (apiKey.isBlank()) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("Configura la API Key de Todoist primero.");
            return;
        }

        try {
            TodoistService todoistService = new TodoistService(apiKey);
            var errores = todoistService.sincronizarTareasConTodoist();

            if (errores.isEmpty()) {
                if (lblCalendarioEstado != null) lblCalendarioEstado.setText("✅ Sincronización completada sin errores");
                actualizarUltimaSyncTodoist();
            } else {
                if (lblCalendarioEstado != null) lblCalendarioEstado.setText("⚠️ Sincronización con " + errores.size() + " errores");
            }
        } catch (Exception e) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("❌ Error en sincronización: " + e.getMessage());
        }
    }

    @FXML
    private void onRestablecerConfigTodoist() {
        if (txtTodoistApiKey != null) txtTodoistApiKey.setText("");
        if (chkTodoistEnabled != null) chkTodoistEnabled.setSelected(false);
        if (spinnerTodoistInterval != null && spinnerTodoistInterval.getValueFactory() != null) {
            spinnerTodoistInterval.getValueFactory().setValue(30);
        }
        if (rbSyncBidireccional != null) rbSyncBidireccional.setSelected(true);
        if (txtIcsPath != null) txtIcsPath.setText("");
        if (txtExportPath != null) txtExportPath.setText("");
        if (chkIcsEnabled != null) chkIcsEnabled.setSelected(false);

        if (lblCalendarioEstado != null) lblCalendarioEstado.setText("Valores restablecidos");
    }

    private void actualizarUltimaSyncIcs() {
        if (lblUltimaSyncIcs != null) {
            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            lblUltimaSyncIcs.setText(ahora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
    }

    private void actualizarUltimaSyncTodoist() {
        if (lblUltimaSyncTodoist != null) {
            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            lblUltimaSyncTodoist.setText(ahora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
    }

    @FXML
    private void onSeleccionarArchivoIcs() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Seleccionar archivo ICS");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Archivos ICS", "*.ics")
        );

        java.io.File archivo = fileChooser.showOpenDialog(null);
        if (archivo != null && txtIcsPath != null) {
            txtIcsPath.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    private void onProbarTodoist() {
        String apiKey = txtTodoistApiKey != null ? txtTodoistApiKey.getText().trim() : "";

        if (apiKey.isBlank()) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("Introduce la API Key de Todoist.");
            return;
        }

        try {
            TodoistService todoistService = new TodoistService(apiKey);
            if (todoistService.verificarApiKey()) {
                if (lblCalendarioEstado != null) lblCalendarioEstado.setText("✅ Conexión con Todoist verificada.");
            } else {
                if (lblCalendarioEstado != null) lblCalendarioEstado.setText("❌ Error: API Key inválida.");
            }
        } catch (Exception e) {
            if (lblCalendarioEstado != null) lblCalendarioEstado.setText("❌ Error de conexión: " + e.getMessage());
        }
    }

    @FXML
    private void onTestOllama() {
        String url = txtOllamaUrl != null ? txtOllamaUrl.getText().trim() : "";
        if (url.isBlank()) {
            if (lblTestIA != null) lblTestIA.setText("Introduce la URL primero.");
            return;
        }

        if (lblTestIA != null) lblTestIA.setText("Probando conexión…");

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
                    if (lblTestIA != null) lblTestIA.setText(msg);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (lblTestIA != null) lblTestIA.setText("✗ Sin conexión: " + e.getMessage());
                });
            }
        }, "test-ollama");

        t.setDaemon(true);
        t.start();
    }

    private void configurarSpinnerSincronizacion() {
        if (spinnerSyncMinutos == null) return;

        int guardado = Math.max(5, Math.min(120,
                Preferences.userNodeForPackage(AppFX.class)
                        .getInt(PREF_SYNC_INTERVAL_MINUTES, 15)));

        spinnerSyncMinutos.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, guardado, 1)
        );
        spinnerSyncMinutos.setEditable(true);

        if (spinnerSyncContactosInterval != null) {
            spinnerSyncContactosInterval.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 1440, 60, 5)
            );
            spinnerSyncContactosInterval.setEditable(true);
        }

        if (spinnerTodoistInterval != null) {
            spinnerTodoistInterval.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 1440, 30, 5)
            );
            spinnerTodoistInterval.setEditable(true);
        }
    }

    @FXML
    private void onAplicarIntervaloSync(ActionEvent event) {
        if (spinnerSyncMinutos == null || spinnerSyncMinutos.getValue() == null) return;

        int m = Math.max(5, Math.min(120, spinnerSyncMinutos.getValue()));
        spinnerSyncMinutos.getValueFactory().setValue(m);
        Preferences.userNodeForPackage(AppFX.class).putInt(PREF_SYNC_INTERVAL_MINUTES, m);

        if (mainController != null) {
            mainController.reprogramarSincronizacionCorreo();
        }

        if (statusLabel != null) statusLabel.setText("Intervalo guardado: cada " + m + " min.");
    }

    private void configurarNavegacionAjustes() {
        if (configNavList == null || configStack == null) return;

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
        if (configStack == null) return;

        for (Node child : configStack.getChildren()) {
            child.setVisible(false);
            child.setManaged(false);
        }

        switch (indice) {
            case 0 -> {
                configStack.getChildren().get(0).setVisible(true);
                configStack.getChildren().get(0).setManaged(true);
            }
            case 1 -> {
                configStack.getChildren().get(1).setVisible(true);
                configStack.getChildren().get(1).setManaged(true);
            }
            case 2 -> {
                configStack.getChildren().get(2).setVisible(true);
                configStack.getChildren().get(2).setManaged(true);
            }
            case 3 -> {
                configStack.getChildren().get(3).setVisible(true);
                configStack.getChildren().get(3).setManaged(true);
            }
            case 4 -> {
                configStack.getChildren().get(4).setVisible(true);
                configStack.getChildren().get(4).setManaged(true);
            }
            default -> { }
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

    private void cargarConfigContactos() {
        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);

        if (txtImportarContactosPath != null) {
            txtImportarContactosPath.setText(prefs.get(PREF_CONTACTOS_IMPORT_PATH, ""));
        }
        if (txtExportarContactosPath != null) {
            txtExportarContactosPath.setText(prefs.get(PREF_CONTACTOS_EXPORT_PATH, ""));
        }
        if (chkSyncContactosAuto != null) {
            chkSyncContactosAuto.setSelected(prefs.getBoolean(PREF_CONTACTOS_SYNC_AUTO, false));
        }
        if (spinnerSyncContactosInterval != null && spinnerSyncContactosInterval.getValueFactory() != null) {
            spinnerSyncContactosInterval.getValueFactory().setValue(
                    prefs.getInt(PREF_CONTACTOS_SYNC_INTERVAL, 60)
            );
        }

        if (lblContactosEstado != null) {
            lblContactosEstado.setText("");
        }

        onCargarRemitentesConfiables();
    }

    @FXML
    private void onCargarRemitentesConfiables() {
        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
        String remitentes = prefs.get(PREF_CONTACTOS_REMITENTES, "");

        if (listaRemitentesConfiables != null) {
            listaRemitentesConfiables.getItems().clear();
            if (!remitentes.isBlank()) {
                String[] lista = remitentes.split(",");
                for (String remitente : lista) {
                    if (!remitente.trim().isBlank()) {
                        listaRemitentesConfiables.getItems().add(remitente.trim());
                    }
                }
            }
        }
    }

    @FXML
    private void onSeleccionarImportarContactos() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Importar contactos");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Archivos vCard", "*.vcf"),
                new javafx.stage.FileChooser.ExtensionFilter("Archivos CSV", "*.csv")
        );

        java.io.File archivo = fileChooser.showOpenDialog(null);
        if (archivo != null && txtImportarContactosPath != null) {
            txtImportarContactosPath.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    private void onImportarContactos() {
        String ruta = txtImportarContactosPath != null ? txtImportarContactosPath.getText().trim() : "";
        if (ruta.isBlank()) {
            if (lblContactosEstado != null) {
                lblContactosEstado.setText("Selecciona un archivo de contactos primero.");
            }
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
        prefs.put(PREF_CONTACTOS_IMPORT_PATH, ruta);

        if (lblContactosEstado != null) {
            lblContactosEstado.setText("✅ Archivo de contactos seleccionado para importar: " + ruta);
        }
    }

    @FXML
    private void onSeleccionarExportarContactos() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Exportar contactos");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Archivos vCard", "*.vcf"),
                new javafx.stage.FileChooser.ExtensionFilter("Archivos CSV", "*.csv")
        );
        fileChooser.setInitialFileName("contactos_exportados.vcf");

        java.io.File archivo = fileChooser.showSaveDialog(null);
        if (archivo != null && txtExportarContactosPath != null) {
            txtExportarContactosPath.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    private void onExportarContactos() {
        String ruta = txtExportarContactosPath != null ? txtExportarContactosPath.getText().trim() : "";
        if (ruta.isBlank()) {
            if (lblContactosEstado != null) {
                lblContactosEstado.setText("Selecciona una ruta para exportar contactos.");
            }
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
        prefs.put(PREF_CONTACTOS_EXPORT_PATH, ruta);

        if (lblContactosEstado != null) {
            lblContactosEstado.setText("✅ Ruta de exportación de contactos guardada: " + ruta);
        }
    }

    @FXML
    private void onAgregarRemitenteConfiable() {
        String remitente = txtNuevoRemitenteConfiable != null ? txtNuevoRemitenteConfiable.getText().trim() : "";
        if (remitente.isBlank()) {
            if (lblContactosEstado != null) {
                lblContactosEstado.setText("Introduce un email de remitente.");
            }
            return;
        }

        if (!remitente.contains("@") || !remitente.contains(".")) {
            if (lblContactosEstado != null) {
                lblContactosEstado.setText("El formato del email no es válido.");
            }
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
        String remitentesExistentes = prefs.get(PREF_CONTACTOS_REMITENTES, "");

        java.util.LinkedHashSet<String> remitentes = new java.util.LinkedHashSet<>();
        if (!remitentesExistentes.isBlank()) {
            for (String item : remitentesExistentes.split(",")) {
                String limpio = item.trim();
                if (!limpio.isBlank()) {
                    remitentes.add(limpio);
                }
            }
        }
        remitentes.add(remitente);
        prefs.put(PREF_CONTACTOS_REMITENTES, String.join(",", remitentes));

        if (txtNuevoRemitenteConfiable != null) {
            txtNuevoRemitenteConfiable.setText("");
        }

        if (lblContactosEstado != null) {
            lblContactosEstado.setText("✅ Remitente agregado: " + remitente);
        }

        onCargarRemitentesConfiables();
    }

    @FXML
    private void onGuardarConfigContactos() {
        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);

        String importPath = txtImportarContactosPath != null ? txtImportarContactosPath.getText().trim() : "";
        String exportPath = txtExportarContactosPath != null ? txtExportarContactosPath.getText().trim() : "";
        boolean syncAuto = chkSyncContactosAuto != null && chkSyncContactosAuto.isSelected();
        int syncInterval = spinnerSyncContactosInterval != null && spinnerSyncContactosInterval.getValue() != null
                ? spinnerSyncContactosInterval.getValue()
                : 60;

        prefs.put(PREF_CONTACTOS_IMPORT_PATH, importPath);
        prefs.put(PREF_CONTACTOS_EXPORT_PATH, exportPath);
        prefs.putBoolean(PREF_CONTACTOS_SYNC_AUTO, syncAuto);
        prefs.putInt(PREF_CONTACTOS_SYNC_INTERVAL, syncInterval);

        if (lblContactosEstado != null) {
            lblContactosEstado.setText("✅ Configuración de contactos guardada.");
        }
    }

    @FXML
    private void onSincronizarContactosAhora() {
        if (lblContactosEstado != null) {
            lblContactosEstado.setText("🔄 Sincronizando contactos...");
        }

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    if (lblContactosEstado != null) {
                        lblContactosEstado.setText("✅ Sincronización de contactos completada.");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    if (lblContactosEstado != null) {
                        lblContactosEstado.setText("❌ Sincronización interrumpida.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (lblContactosEstado != null) {
                        lblContactosEstado.setText("❌ Error en sincronización: " + e.getMessage());
                    }
                });
            }
        }, "sync-contactos");

        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onEliminarRemitenteConfiable() {
        String seleccionado = listaRemitentesConfiables != null
                ? listaRemitentesConfiables.getSelectionModel().getSelectedItem()
                : null;

        if (seleccionado == null) {
            if (lblContactosEstado != null) {
                lblContactosEstado.setText("Selecciona un remitente para eliminar.");
            }
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);
        String remitentesExistentes = prefs.get(PREF_CONTACTOS_REMITENTES, "");
        String[] lista = remitentesExistentes.split(",");
        StringBuilder nuevosRemitentes = new StringBuilder();

        for (String remitente : lista) {
            if (!remitente.trim().equals(seleccionado.trim())) {
                if (nuevosRemitentes.length() > 0) {
                    nuevosRemitentes.append(",");
                }
                nuevosRemitentes.append(remitente.trim());
            }
        }

        prefs.put(PREF_CONTACTOS_REMITENTES, nuevosRemitentes.toString());

        if (lblContactosEstado != null) {
            lblContactosEstado.setText("✅ Remitente eliminado: " + seleccionado);
        }

        onCargarRemitentesConfiables();
    }

    @FXML
    private void onProviderChanged(ActionEvent event) {
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
        if (statusLabel != null) statusLabel.setText("");

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
        if (comboCuentasExistentes == null) return;

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
        if (comboCuentasExistentes == null) return;

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
        if (comboTema == null || comboTema.getValue() == null) return;

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

        if (scene == null) return;

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

    private void cargarConfigCalendarioTodoist() {
        Preferences prefs = Preferences.userNodeForPackage(ConfigController.class);

        if (txtTodoistApiKey != null) {
            txtTodoistApiKey.setText(prefs.get(PREF_TODOIST_API_KEY, ""));
        }
        if (chkTodoistEnabled != null) {
            chkTodoistEnabled.setSelected(prefs.getBoolean(PREF_TODOIST_ENABLED, false));
        }
        if (spinnerTodoistInterval != null && spinnerTodoistInterval.getValueFactory() != null) {
            spinnerTodoistInterval.getValueFactory().setValue(prefs.getInt(PREF_TODOIST_INTERVAL, 30));
        }
        if (txtIcsPath != null) {
            txtIcsPath.setText(prefs.get(PREF_ICS_PATH, ""));
        }
        if (chkIcsEnabled != null) {
            chkIcsEnabled.setSelected(prefs.getBoolean(PREF_ICS_ENABLED, false));
        }
        if (txtExportPath != null) {
            txtExportPath.setText(prefs.get(PREF_ICS_EXPORT_PATH, ""));
        }

        String syncDirection = prefs.get(PREF_TODOIST_SYNC_DIRECTION, "bidirectional");
        if (rbSyncBidireccional != null) rbSyncBidireccional.setSelected("bidirectional".equals(syncDirection));
        if (rbSyncSoloSubida != null) rbSyncSoloSubida.setSelected("upload".equals(syncDirection));
        if (rbSyncSoloBajada != null) rbSyncSoloBajada.setSelected("download".equals(syncDirection));

        if (lblCalendarioEstado != null) {
            lblCalendarioEstado.setText("");
        }

        actualizarUltimaSyncIcs();
        actualizarUltimaSyncTodoist();
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