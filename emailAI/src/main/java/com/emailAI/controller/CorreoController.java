package com.emailAI.controller;

import com.emailAI.dao.DAOMensajes;
import com.emailAI.dao.DAORemitentesConfiables;
import com.emailAI.model.Mensaje;
import com.emailAI.service.MailService;
import com.emailAI.service.SpamIaService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.awt.Desktop;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// Controla la bandeja de entrada, detalle de mensajes y acciones de IA sobre el correo.
public class CorreoController {

    @FXML
    private ListView<Mensaje> lstMensajes;

    @FXML
    private Label lblAsunto;

    @FXML
    private Label lblRemitente;

    @FXML
    private Label lblEstadoApp;

    @FXML
    private Label lblEstadoIA;

    @FXML
    private WebView webViewCuerpo;

    @FXML
    private TextArea txtCuerpo;

    @FXML
    private Button btnReentrenar;

    @FXML
    private Button btnSpam;

    @FXML
    private Button btnLegitimo;

    @FXML
    private Button btnSug1;

    @FXML
    private Button btnSug2;

    @FXML
    private Button btnSug3;

    @FXML
    private Button btnAnularSub;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnBorrarSeleccionado;

    @FXML
    private Button btnPermitirImagenes;

    private MailService mailService;
    private SpamIaService spamIaService;
    private DAOMensajes daoMensajes;
    private DAORemitentesConfiables daoRemitentes;
    private String cuentaHash;

    private final ObservableList<Mensaje> modeloMensajes = FXCollections.observableArrayList();
    private Mensaje mensajeActual;

    private String carpetaImapActiva = "INBOX";

    private final Set<String> mensajesConImagenesPermitidas = new HashSet<>();

    private final WebLinkBridge webLinkBridge = new WebLinkBridge(this::abrirEnNavegadorSeguro);

    public static final class WebLinkBridge {
        private final Consumer<String> onUrl;

        public WebLinkBridge(Consumer<String> onUrl) {
            this.onUrl = onUrl;
        }

        public void openExternal(String url) {
            Platform.runLater(() -> onUrl.accept(url));
        }
    }

    private boolean esConfiableSeguro(String remitente) {
        try {
            return daoRemitentes != null && daoRemitentes.esConfiable(remitente);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void initialize() {
        lstMensajes.setItems(modeloMensajes);

        lstMensajes.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Mensaje msg, boolean empty) {
                super.updateItem(msg, empty);

                getStyleClass().removeAll(
                        "message-card",
                        "message-card-spam",
                        "message-card-risk",
                        "message-card-legit",
                        "message-card-pending"
                );

                if (empty || msg == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String remitente = msg.getRemitente() != null ? msg.getRemitente() : "";
                String asunto = msg.getAsunto() != null ? msg.getAsunto() : "";

                setText(remitente + "\n" + asunto);

                String cat = msg.getCategoria();

                if (cat == null || cat.isBlank() || "DESCONOCIDO".equalsIgnoreCase(cat) || "PENDIENTE".equalsIgnoreCase(cat)) {
                    getStyleClass().add("message-card-pending");
                } else if ("SPAM".equalsIgnoreCase(cat) || "PHISHING".equalsIgnoreCase(cat)) {
                    getStyleClass().add("message-card-risk");
                } else if ("LEGITIMO".equalsIgnoreCase(cat) || "LEGIT".equalsIgnoreCase(cat)) {
                    getStyleClass().add("message-card-legit");
                } else {
                    getStyleClass().add("message-card-pending");
                }
            }
        });

        lstMensajes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> mostrarMensaje(newSel)
        );

        configurarWebViewEnlacesExternos();
        actualizarSugerenciasIA(null);
    }

    private void configurarWebViewEnlacesExternos() {
        WebEngine engine = webViewCuerpo.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) return;

            try {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("emailApp", webLinkBridge);

                String script =
                        "(function(){"
                                + "function findAnchor(el){"
                                + "var n=el;"
                                + "while(n){if(n.tagName==='A')return n;n=n.parentNode;}"
                                + "return null;"
                                + "}"
                                + "document.addEventListener('click',function(e){"
                                + "var a=findAnchor(e.target);"
                                + "if(!a)return;"
                                + "var href=a.getAttribute('href');"
                                + "if(!href)return;"
                                + "var h=href.trim();"
                                + "var lo=h.toLowerCase();"
                                + "if(lo.indexOf('http://')===0||lo.indexOf('https://')===0){"
                                + "e.preventDefault();"
                                + "if(e.stopPropagation)e.stopPropagation();"
                                + "if(e.stopImmediatePropagation)e.stopImmediatePropagation();"
                                + "emailApp.openExternal(h);"
                                + "}"
                                + "},true);"
                                + "})();";

                engine.executeScript(script);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void abrirEnNavegadorSeguro(String url) {
        if (url == null) return;

        String u = url.trim();
        String lo = u.toLowerCase();
        if (!lo.startsWith("http://") && !lo.startsWith("https://")) {
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(u));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCarpetaImap(String imapFullName) {
        if (imapFullName == null || imapFullName.isBlank()) {
            imapFullName = "INBOX";
        }
        this.carpetaImapActiva = imapFullName;
        cargarDesdeBD();
        cargarBandejaEntradaAsync();
    }

    public String getCarpetaImapActiva() {
        return carpetaImapActiva;
    }

    public void sincronizarEnFondoSiBandejaEntrada() {
        if (!"INBOX".equalsIgnoreCase(carpetaImapActiva)) {
            return;
        }
        if (mailService == null || daoMensajes == null || cuentaHash == null) {
            return;
        }
        cargarBandejaEntradaAsync();
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;

        if (mailService == null) {
            return;
        }

        this.cuentaHash = mailService.getCuentaHash();
        this.spamIaService = mailService.getSpamIaService();

        try {
            this.daoMensajes = new DAOMensajes();
            this.daoRemitentes = new DAORemitentesConfiables();

            cargarDesdeBD();
            cargarBandejaEntradaAsync();
        } catch (SQLException e) {
            e.printStackTrace();
            if (lblEstadoApp != null) {
                lblEstadoApp.setText("Error inicializando base de datos de correo.");
            }
        }
    }

    private void cargarDesdeBD() {
        if (daoMensajes == null || cuentaHash == null) return;

        try {
            List<Mensaje> lista = daoMensajes.listarPorCuentaHashYCarpeta(cuentaHash, carpetaImapActiva);
            modeloMensajes.setAll(lista);

            if (!modeloMensajes.isEmpty()) {
                lstMensajes.getSelectionModel().selectFirst();
            } else {
                limpiarDetalle();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            modeloMensajes.clear();
            limpiarDetalle();

            if (lblEstadoApp != null) {
                lblEstadoApp.setText("Error cargando mensajes desde la base de datos.");
            }
        }
    }

    public void cargarBandejaEntradaAsync() {
        if (mailService == null || daoMensajes == null || cuentaHash == null) return;

        String uidSeleccionado = null;
        Mensaje actual = lstMensajes.getSelectionModel().getSelectedItem();
        if (actual != null) {
            uidSeleccionado = actual.getUidImap();
        }
        final String uidSelFinal = uidSeleccionado;
        final String carpetaSync = carpetaImapActiva;

        if (lblEstadoApp != null) {
            lblEstadoApp.setText("Actualizando bandeja...");
        }

        Task<List<Mensaje>> task = new Task<>() {
            @Override
            protected List<Mensaje> call() {
                try {
                    List<Mensaje> mensajesImap = mailService.listMensajesDeCarpeta(carpetaSync, 20);
                    daoMensajes.guardarOModificar(mensajesImap, cuentaHash, carpetaSync);
                    return mensajesImap;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        task.setOnSucceeded(ev -> {
            List<Mensaje> nuevos = task.getValue();
            if (nuevos == null) {
                if (lblEstadoApp != null) {
                    lblEstadoApp.setText("No se pudieron obtener mensajes.");
                }
                return;
            }

            modeloMensajes.setAll(nuevos);

            if (uidSelFinal != null) {
                for (Mensaje m : modeloMensajes) {
                    if (uidSelFinal.equals(m.getUidImap())) {
                        lstMensajes.getSelectionModel().select(m);
                        if (lblEstadoApp != null) {
                            lblEstadoApp.setText("Bandeja actualizada.");
                        }
                        return;
                    }
                }
            }

            if (!modeloMensajes.isEmpty()) {
                lstMensajes.getSelectionModel().selectFirst();
            } else {
                limpiarDetalle();
            }

            if (lblEstadoApp != null) {
                lblEstadoApp.setText("Bandeja actualizada.");
            }
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
            if (lblEstadoApp != null) {
                lblEstadoApp.setText("Error cargando la bandeja.");
            }
        });

        Thread t = new Thread(task, "cargar-bandeja-imap");
        t.setDaemon(true);
        t.start();
    }

    private void limpiarDetalle() {
        lblAsunto.setText("");
        lblRemitente.setText("");
        webViewCuerpo.getEngine().loadContent("");
        txtCuerpo.clear();
        mensajeActual = null;

        if (btnPermitirImagenes != null) {
            btnPermitirImagenes.setDisable(true);
        }

        actualizarSugerenciasIA(null);
    }

    private void mostrarMensaje(Mensaje msg) {
        mensajeActual = msg;

        if (msg == null) {
            limpiarDetalle();
            return;
        }

        String asunto = msg.getAsunto() != null ? msg.getAsunto() : "";
        String remitente = msg.getRemitente() != null ? msg.getRemitente() : "";

        lblAsunto.setText(asunto);
        lblRemitente.setText(remitente);

        String html = msg.getHtml();
        String texto = msg.getCuerpo();

        if (html != null && !html.isBlank()) {
            boolean remitenteConfiable = !remitente.isBlank() && esConfiableSeguro(remitente);
            boolean imagenesPermitidasPorUsuario = msg.getUidImap() != null
                    && mensajesConImagenesPermitidas.contains(msg.getUidImap());

            boolean permitirImagenes = remitenteConfiable || imagenesPermitidasPorUsuario;
            boolean tieneImagenesExternas = contieneImagenesExternas(html);

            String htmlRender = permitirImagenes ? html : eliminarImagenesExternas(html);

            webViewCuerpo.setVisible(true);
            webViewCuerpo.setManaged(true);
            webViewCuerpo.getEngine().loadContent(htmlRender, "text/html");

            txtCuerpo.setVisible(false);
            txtCuerpo.setManaged(false);
            txtCuerpo.clear();

            if (btnPermitirImagenes != null) {
                btnPermitirImagenes.setDisable(permitirImagenes || !tieneImagenesExternas);
            }
        } else {
            txtCuerpo.setVisible(true);
            txtCuerpo.setManaged(true);
            txtCuerpo.setText(texto != null ? texto : "");

            webViewCuerpo.setVisible(false);
            webViewCuerpo.setManaged(false);
            webViewCuerpo.getEngine().loadContent("");

            if (btnPermitirImagenes != null) {
                btnPermitirImagenes.setDisable(true);
            }
        }

        actualizarSugerenciasIA(msg);
    }

    private String eliminarImagenesExternas(String html) {
        if (html == null || html.isBlank()) return html;

        return html.replaceAll(
                "(?is)<img\\b[^>]*\\bsrc\\s*=\\s*(['\"]?)https?://.*?\\1[^>]*>",
                ""
        );
    }

    private boolean contieneImagenesExternas(String html) {
        if (html == null || html.isBlank()) return false;

        return html.matches(
                "(?is).*<img\\b[^>]*\\bsrc\\s*=\\s*(['\"]?)https?://.*?\\1[^>]*>.*"
        );
    }

    @FXML
    private void onPermitirImagenes() {
        if (mensajeActual == null) return;

        String uid = mensajeActual.getUidImap();
        if (uid == null || uid.isBlank()) return;

        mensajesConImagenesPermitidas.add(uid);
        mostrarMensaje(mensajeActual);
    }

    private void abrirVentanaCompose(Consumer<ComposeController> init) {
        try {
            var url = getClass().getResource("/ui/compose-view.fxml");
            if (url == null) {
                System.err.println("No se encontró /ui/compose-view.fxml en el classpath");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());

            ComposeController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Redactar correo");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            controller.setMailService(mailService);
            controller.setStage(stage);

            if (init != null) {
                init.accept(controller);
            }

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRedactar() {
        if (mailService == null) return;
        abrirVentanaCompose(ComposeController::inicializarNuevo);
    }

    @FXML
    private void onActualizarBandeja() {
        cargarDesdeBD();
        cargarBandejaEntradaAsync();
    }

    @FXML
    private void onResponder() {
        if (mailService == null) return;
        Mensaje seleccionado = lstMensajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        abrirVentanaCompose(c -> c.inicializarResponder(seleccionado));
    }

    @FXML
    private void onResponderTodos() {
        if (mailService == null) return;
        Mensaje seleccionado = lstMensajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        abrirVentanaCompose(c -> c.inicializarResponderTodos(seleccionado));
    }

    @FXML
    private void onReenviar() {
        if (mailService == null) return;
        Mensaje seleccionado = lstMensajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        abrirVentanaCompose(c -> c.inicializarReenviar(seleccionado));
    }

    @FXML
    private void onBorrarMensajeSeleccionado() {
        Mensaje seleccionado = lstMensajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null || mailService == null) {
            return;
        }

        try {
            mailService.eliminarMensaje(seleccionado, carpetaImapActiva);
            modeloMensajes.remove(seleccionado);

            if (!modeloMensajes.isEmpty()) {
                lstMensajes.getSelectionModel().selectFirst();
            } else {
                limpiarDetalle();
            }

            if (lblEstadoApp != null) {
                lblEstadoApp.setText("Mensaje eliminado.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (lblEstadoApp != null) {
                lblEstadoApp.setText("Error eliminando el mensaje.");
            }
        }
    }

    @FXML
    private void onMarcarSpam() {
        Mensaje seleccionado = lstMensajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        seleccionado.setCategoria("SPAM");
        seleccionado.setPrioridad("NORMAL");

        try {
            if (daoMensajes != null && cuentaHash != null) {
                daoMensajes.actualizarCategoriaPrioridad(
                        seleccionado.getUidImap(),
                        cuentaHash,
                        carpetaImapActiva,
                        seleccionado.getCategoria(),
                        seleccionado.getPrioridad()
                );

                lstMensajes.refresh();
                mostrarMensaje(seleccionado);

                if (lblEstadoApp != null) {
                    lblEstadoApp.setText("Mensaje marcado como spam.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Base de Datos");
            alert.setHeaderText("No se pudo actualizar la categoría.");
            alert.setContentText("Hubo un problema al guardar el mensaje: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onMarcarLegitimo() {
        Mensaje seleccionado = lstMensajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        seleccionado.setCategoria("LEGITIMO");
        seleccionado.setPrioridad("ALTA");

        try {
            if (daoMensajes != null && cuentaHash != null) {
                daoMensajes.actualizarCategoriaPrioridad(
                        seleccionado.getUidImap(),
                        cuentaHash,
                        carpetaImapActiva,
                        seleccionado.getCategoria(),
                        seleccionado.getPrioridad()
                );

                lstMensajes.refresh();
                mostrarMensaje(seleccionado);

                if (lblEstadoApp != null) {
                    lblEstadoApp.setText("Mensaje marcado como legítimo.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Base de Datos");
            alert.setHeaderText("No se pudo actualizar la categoría.");
            alert.setContentText("Hubo un problema al guardar el mensaje: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onReentrenarModelo() {
        entrenarConEjemplos();
    }

    private void entrenarConEjemplos() {
        if (spamIaService == null || daoMensajes == null || cuentaHash == null) return;

        if (lblEstadoIA != null) {
            lblEstadoIA.setText("Reentrenando modelo...");
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<Mensaje> todos = daoMensajes.listarTodosPorCuenta(cuentaHash);
                    List<Mensaje> etiquetados = todos.stream()
                            .filter(m -> m.getCategoria() != null
                                    && !m.getCategoria().isBlank()
                                    && !"DESCONOCIDO".equalsIgnoreCase(m.getCategoria()))
                            .collect(Collectors.toList());

                    spamIaService.entrenarModelo(cuentaHash, etiquetados);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        task.setOnSucceeded(ev -> {
            if (lblEstadoIA != null) {
                lblEstadoIA.setText("Modelo reentrenado.");
            }
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
            if (lblEstadoIA != null) {
                lblEstadoIA.setText("Error al reentrenar el modelo.");
            }
        });

        Thread t = new Thread(task, "entrenar-spam-model");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onUsarRespuesta1() {
        usarRespuestaSugerida(btnSug1);
    }

    @FXML
    private void onUsarRespuesta2() {
        usarRespuestaSugerida(btnSug2);
    }

    @FXML
    private void onUsarRespuesta3() {
        usarRespuestaSugerida(btnSug3);
    }

    @FXML
    private void onAnularSub() {
        if (mensajeActual == null) {
            if (lblEstadoIA != null) {
                lblEstadoIA.setText("Selecciona un mensaje primero.");
            }
            return;
        }

        abrirVentanaCompose(c -> {
            c.inicializarResponder(mensajeActual);
            c.setCuerpoInicial(
                    "Hola,\n\n" +
                    "Quiero darme de baja de esta suscripción y dejar de recibir estos correos.\n\n" +
                    "Gracias.\n"
            );
        });

        if (lblEstadoIA != null) {
            lblEstadoIA.setText("Borrador de baja preparado.");
        }
    }

    private void usarRespuestaSugerida(Button boton) {
        if (mensajeActual == null) {
            if (lblEstadoIA != null) {
                lblEstadoIA.setText("Selecciona un mensaje primero.");
            }
            return;
        }

        if (boton == null) {
            return;
        }

        Object payload = boton.getUserData();
        String sugerencia = payload != null ? payload.toString() : null;

        if (sugerencia == null || sugerencia.isBlank()) {
            if (lblEstadoIA != null) {
                lblEstadoIA.setText("No hay sugerencia disponible.");
            }
            return;
        }

        abrirVentanaCompose(c -> {
            c.inicializarResponder(mensajeActual);
            c.setCuerpoInicial(sugerencia);
        });

        if (lblEstadoIA != null) {
            lblEstadoIA.setText("Sugerencia aplicada al borrador.");
        }
    }

    private void actualizarSugerenciasIA(Mensaje msg) {
        if (btnSug1 == null || btnSug2 == null || btnSug3 == null || btnAnularSub == null) {
            return;
        }

        if (msg == null) {
            btnSug1.setText("");
            btnSug2.setText("");
            btnSug3.setText("");

            btnSug1.setUserData(null);
            btnSug2.setUserData(null);
            btnSug3.setUserData(null);

            btnSug1.setDisable(true);
            btnSug2.setDisable(true);
            btnSug3.setDisable(true);
            btnAnularSub.setDisable(true);

            if (lblEstadoIA != null) {
                lblEstadoIA.setText("IA: sin mensaje seleccionado");
            }
            return;
        }

        String asunto = msg.getAsunto() != null ? msg.getAsunto().toLowerCase() : "";
        String cuerpo = msg.getCuerpo() != null ? msg.getCuerpo().toLowerCase() : "";
        String remitente = msg.getRemitente() != null ? msg.getRemitente() : "";

        boolean pareceNewsletter =
                asunto.contains("newsletter") ||
                asunto.contains("suscripción") ||
                asunto.contains("oferta") ||
                asunto.contains("promoción") ||
                cuerpo.contains("unsubscribe") ||
                cuerpo.contains("cancelar suscripción") ||
                cuerpo.contains("darse de baja");

        boolean parecePeticion =
                cuerpo.contains("?") ||
                cuerpo.contains("¿") ||
                asunto.contains("consulta") ||
                asunto.contains("pregunta") ||
                asunto.contains("podrías") ||
                asunto.contains("puedes");

        String titulo1;
        String titulo2;
        String titulo3;

        String texto1;
        String texto2;
        String texto3;

        if (pareceNewsletter) {
            titulo1 = "No me interesa";
            titulo2 = "Eliminar de lista";
            titulo3 = "No enviar más";

            texto1 = "Hola,\n\nGracias, pero no me interesa esta información en este momento.\n\nUn saludo.";
            texto2 = "Hola,\n\nPor favor, eliminad mi dirección de vuestra lista de distribución.\n\nGracias.";
            texto3 = "Hola,\n\nHe recibido vuestro correo. No es necesario que me enviéis más mensajes de este tipo.\n\nUn saludo.";

            btnAnularSub.setDisable(false);
        } else if (parecePeticion) {
            titulo1 = "Confirmar recepción";
            titulo2 = "Pedir más detalles";
            titulo3 = "Responder más tarde";

            texto1 = "Hola,\n\nGracias por tu mensaje. Lo reviso y te respondo en breve.\n\nUn saludo.";
            texto2 = "Hola,\n\n¿Podrías darme un poco más de detalle para poder ayudarte mejor?\n\nGracias.";
            texto3 = "Hola,\n\nRecibido. En cuanto lo revise te digo algo.\n\nUn saludo.";

            btnAnularSub.setDisable(true);
        } else {
            titulo1 = "Tomar nota";
            titulo2 = "Revisar luego";
            titulo3 = "Confirmar recibido";

            texto1 = "Hola,\n\nGracias por tu correo. Tomo nota.\n\nUn saludo.";
            texto2 = "Hola,\n\nHe recibido tu mensaje y lo revisaré en cuanto pueda.\n\nGracias.";
            texto3 = "Hola,\n\nPerfecto, queda anotado.\n\nUn saludo.";

            btnAnularSub.setDisable(true);
        }

        btnSug1.setText(titulo1);
        btnSug2.setText(titulo2);
        btnSug3.setText(titulo3);

        btnSug1.setUserData(texto1);
        btnSug2.setUserData(texto2);
        btnSug3.setUserData(texto3);

        btnSug1.setDisable(false);
        btnSug2.setDisable(false);
        btnSug3.setDisable(false);

        if (lblEstadoIA != null) {
            lblEstadoIA.setText("IA: sugerencias listas para " + (remitente.isBlank() ? "el mensaje seleccionado" : remitente));
        }
    }
}