package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.service.MailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainController {

    @FXML private BorderPane rootPane;

    // Menú lateral
    @FXML private ToggleButton btnCorreo;
    @FXML private ToggleButton btnCalendario;
    @FXML private ToggleButton btnContactos;
    @FXML private ToggleButton btnTareas;
    @FXML private ToggleButton btnConfiguracion;
    @FXML private ToggleButton btnChatIA;
    @FXML private ToggleGroup grpSecciones;

    // Botón de tema sol/luna
    @FXML private Button btnTema;

    // Estado (lo muestra la barra superior de correo)
    @FXML private Label lblEstado;

    private MailService mailService;
    private boolean modoOscuro = true;

    // Lo llama LoginController al abrir main-view.fxml
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
        cargarVistaCorreo();   // correo como vista inicial
    }

    @FXML
    private void initialize() {
        // Selecciona Correo por defecto en la barra lateral
        if (grpSecciones != null && btnCorreo != null) {
            grpSecciones.selectToggle(btnCorreo);
        }
        if (btnTema != null) {
            btnTema.setText("☾"); // empezamos en modo oscuro
        }
    }

    // ========= Navegación secciones =========

    @FXML
    private void onSeccionCorreo() {
        if (grpSecciones != null) grpSecciones.selectToggle(btnCorreo);
        cargarVistaCorreo();
    }

    @FXML
    private void onSeccionCalendario() {
        if (grpSecciones != null) grpSecciones.selectToggle(btnCalendario);
        cargarVistaSimple("/ui/calendario-view.fxml", "Calendario");
    }

    @FXML
    private void onSeccionContactos() {
        if (grpSecciones != null) grpSecciones.selectToggle(btnContactos);
        cargarVistaSimple("/ui/contactos-view.fxml", "Contactos");
    }

    @FXML
    private void onSeccionTareas() {
        if (grpSecciones != null) grpSecciones.selectToggle(btnTareas);
        cargarVistaSimple("/ui/tareas-view.fxml", "Tareas");
    }

    @FXML
    private void onSeccionConfiguracion() {
        if (grpSecciones != null) grpSecciones.selectToggle(btnConfiguracion);
        cargarVistaSimple("/ui/config-view.fxml", "Configuración");
    }

    @FXML
    private void onSeccionChatIA() {
        if (grpSecciones != null) grpSecciones.selectToggle(btnChatIA);
        cargarVistaSimple("/ui/chat-ia-view.fxml", "Chat IA");
    }

    // ========= Botón de tema sol / luna =========

    @FXML
    private void onToggleTema() {
        modoOscuro = !modoOscuro;
        if (modoOscuro) {
            btnTema.setText("☾");
            aplicarTemaOscuro();
        } else {
            btnTema.setText("☀");
            aplicarTemaClaro();
        }
    }

    private void aplicarTemaOscuro() {
        if (rootPane.getScene() == null) return;
        rootPane.getScene().getStylesheets().clear();
        rootPane.getScene().getStylesheets().add(
                AppFX.class.getResource("/styles-dark.css").toExternalForm()
        );
    }

    private void aplicarTemaClaro() {
        if (rootPane.getScene() == null) return;
        rootPane.getScene().getStylesheets().clear();
        rootPane.getScene().getStylesheets().add(
                AppFX.class.getResource("/styles-light.css").toExternalForm()
        );
    }

    // ========= Helpers carga de vistas =========

    private void cargarVistaCorreo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/correo-view.fxml"));
            Parent contenido = loader.load();

            // Pasar el MailService al controlador de correo
            CorreoController correoController = loader.getController();
            if (mailService != null) {
                try {
                    correoController.setMailService(mailService);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (lblEstado != null) {
                        lblEstado.setText("Error cargando bandeja: " + e.getMessage());
                    }
                }
            }

            rootPane.setCenter(contenido);

            if (lblEstado != null) {
                lblEstado.setText("Sección: Correo");
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (lblEstado != null) {
                lblEstado.setText("Error cargando Correo: " + e.getMessage());
            }
        }
    }

    private void cargarVistaSimple(String recursoFxml, String nombreSeccion) {
        try {
            Parent contenido = FXMLLoader.load(getClass().getResource(recursoFxml));
            rootPane.setCenter(contenido);
            if (lblEstado != null) {
                lblEstado.setText("Sección: " + nombreSeccion);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (lblEstado != null) {
                lblEstado.setText("Error cargando " + nombreSeccion + ": " + e.getMessage());
            }
        }
    }
}
