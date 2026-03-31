package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.service.MailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

// Controla la ventana principal y la navegación entre secciones (correo, calendario, contactos, etc.).
public class MainController {

    // Contenedor central donde se van insertando las distintas vistas.
    @FXML
    private StackPane centerPane;

    // Botón de navegación hacia la sección de correo.
    @FXML
    private ToggleButton btnCorreo;

    // Botón de navegación hacia la sección de calendario.
    @FXML
    private ToggleButton btnCalendario;

    // Botón de navegación hacia la sección de contactos.
    @FXML
    private ToggleButton btnContactos;

    // Botón de navegación hacia la sección de tareas.
    @FXML
    private ToggleButton btnTareas;

    // Botón de navegación hacia la sección de configuración.
    @FXML
    private ToggleButton btnConfiguracion;

    // Botón de navegación hacia la sección de chat IA.
    @FXML
    private ToggleButton btnChatIA;

    // Grupo de toggle buttons que asegura que solo una sección esté activa.
    @FXML
    private ToggleGroup grpSecciones;

    // Botón que alterna el tema claro/oscuro desde la ventana principal.
    @FXML
    private ToggleButton btnTema;

    private MailService mailService; // Servicio de correo reutilizado por las sub-vistas.
    private boolean temaClaro = false; // Indica si el tema actual es claro.

    // cache de vistas/controladores
    private Node vistaCorreo; // Vista FXML ya cargada para la sección de correo.
    private CorreoController correoController; // Controlador asociado a la vista de correo.

    // ===================== Inicialización =====================

    // Inicializa la vista marcando por defecto la sección de correo.
    @FXML
    private void initialize() {
        if (btnCorreo != null) {
            btnCorreo.setSelected(true);
        }
    }

    // Lo llama LoginController después de conectar
    // Recibe el MailService y configura la sección de correo inicial.
    public void setMailService(MailService mailService) throws Exception {
        this.mailService = mailService;

        // Aplica tema por defecto después de tener la escena
        aplicarTema(temaClaro);

        seleccionarCorreo();
    }

    // ===================== Navegación secciones =====================

    // Handler del botón de sección correo que carga la vista correspondiente.
    @FXML
    private void onSeccionCorreo() {
        try {
            seleccionarCorreo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga (o reutiliza) la vista de correo y la muestra en el centro.
    private void seleccionarCorreo() throws Exception {
        if (vistaCorreo == null) {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/correo-view.fxml"));
            vistaCorreo = loader.load();
            correoController = loader.getController();
            if (mailService != null) {
                correoController.setMailService(mailService);
                // cargarBandejaEntradaAsync ya se llama desde setMailService, no hace falta repetir
            }
        }
        centerPane.getChildren().setAll(vistaCorreo);
        if (btnCorreo != null) {
            btnCorreo.setSelected(true);
        }
    }

    // Carga y muestra la vista de calendario en el centro.
    @FXML
    private void onSeccionCalendario() {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/calendario-view.fxml"));
            Node vista = loader.load();
            centerPane.getChildren().setAll(vista);
            if (btnCalendario != null) btnCalendario.setSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga y muestra la vista de contactos en el centro.
    @FXML
    private void onSeccionContactos() {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/contactos-view.fxml"));
            Node vista = loader.load();
            centerPane.getChildren().setAll(vista);
            if (btnContactos != null) btnContactos.setSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga y muestra la vista de tareas en el centro.
    @FXML
    private void onSeccionTareas() {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/tareas-view.fxml"));
            Node vista = loader.load();
            centerPane.getChildren().setAll(vista);
            if (btnTareas != null) btnTareas.setSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga y muestra la vista de configuración, pasando el MainController para el tema.
    @FXML
    private void onSeccionConfiguracion() {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/config-view.fxml"));
            Node vista = loader.load();

            ConfigController controller = loader.getController();
            controller.setMainController(this);

            centerPane.getChildren().setAll(vista);
            if (btnConfiguracion != null) btnConfiguracion.setSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga y muestra la vista de chat IA en el centro.
    @FXML
    private void onSeccionChatIA() {
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/chat-view.fxml"));
            Node vista = loader.load();
            centerPane.getChildren().setAll(vista);
            if (btnChatIA != null) btnChatIA.setSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== Tema claro/oscuro =====================

    // Indica si el tema global actual es el claro.
    public boolean isTemaClaro() {
        return temaClaro;
    }

    // Aplica tema claro/oscuro sobre la escena actual y actualiza el botón de tema.
    public void aplicarTema(boolean light) {
        this.temaClaro = light;

        Scene scene = centerPane != null ? centerPane.getScene() : null;
        if (scene == null) return;

        scene.getStylesheets().clear();

        // CSS base siempre
        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-basic.css").toExternalForm()
        );

        // Tema claro/oscuro
        if (light) {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-light.css").toExternalForm()
            );
        } else {
            scene.getStylesheets().add(
                    AppFX.class.getResource("/styles-dark.css").toExternalForm()
            );
        }

        if (btnTema != null) {
            btnTema.setSelected(light);
            btnTema.setText(light ? "☼" : "☾");
        }
    }

    // Handler del botón de tema que invierte el modo actual.
    @FXML
    private void onToggleTema() {
        if (btnTema == null) return;
        aplicarTema(btnTema.isSelected());
    }
}