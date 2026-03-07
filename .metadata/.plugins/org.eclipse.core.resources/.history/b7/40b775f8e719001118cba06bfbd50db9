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
import javafx.stage.Stage;

public class MainController {

    @FXML
    private StackPane centerPane;

    @FXML
    private ToggleButton btnCorreo;

    @FXML
    private ToggleButton btnCalendario;

    @FXML
    private ToggleButton btnContactos;

    @FXML
    private ToggleButton btnTareas;

    @FXML
    private ToggleButton btnConfiguracion;

    @FXML
    private ToggleButton btnChatIA;

    @FXML
    private ToggleGroup grpSecciones;

    // Servicio de correo compartido
    private MailService mailService;

    // Llamado desde LoginController después de conectar
    public void setMailService(MailService mailService) throws Exception {
        this.mailService = mailService;
        // Cargar por defecto la sección Correo
        seleccionarCorreo();
    }

    @FXML
    private void initialize() {
        // Si llegamos sin setMailService (caso inicial), al menos marcamos el botón Correo.
        if (btnCorreo != null) {
            btnCorreo.setSelected(true);
        }
    }

    // ===================== Navegación de secciones =====================

    @FXML
    private void onSeccionCorreo() {
        try {
            seleccionarCorreo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seleccionarCorreo() throws Exception {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/correo-view.fxml"));
        Node vistaCorreo = loader.load();

        // Pasar mailService al CorreoController
        CorreoController controller = loader.getController();
        if (mailService != null) {
            controller.setMailService(mailService);
        }

        centerPane.getChildren().setAll(vistaCorreo);
        if (btnCorreo != null) {
            btnCorreo.setSelected(true);
        }
    }

    @FXML
    private void onSeccionCalendario() {
        // TODO: cargar calendario-view.fxml en centerPane
    }

    @FXML
    private void onSeccionContactos() {
        // TODO: cargar contactos-view.fxml en centerPane
    }

    @FXML
    private void onSeccionTareas() {
        // TODO: cargar tareas-view.fxml en centerPane
    }

    @FXML
    private void onSeccionConfiguracion() {
        // TODO: cargar config-view.fxml o una vista específica de config general
    }

    @FXML
    private void onSeccionChatIA() {
        // TODO: cargar vista de chat IA
    }

    @FXML
    private void onToggleTema() {
        // TODO: si quieres cambiar tema desde aquí, similar a LoginController
    }
}
