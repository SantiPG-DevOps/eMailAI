package com.emailAI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MailViewController {

    @FXML
    private Label lblAsunto;

    @FXML
    private Label lblRemitente;

    @FXML
    private WebView webMensaje;

    private WebEngine engine;

    @FXML
    private void initialize() {
        if (webMensaje != null) {
            engine = webMensaje.getEngine();
            // página vacía inicial
            engine.loadContent(
                    "<html><body style='font-family: sans-serif; font-size: 13px;'></body></html>",
                    "text/html"
            );
        }
    }

    /**
     * Mostrar un mensaje en el visor HTML.
     *
     * @param remitente Texto para el remitente (ej: "Nombre <email@dominio>")
     * @param asunto    Asunto del mensaje
     * @param html      Contenido HTML completo del mensaje
     */
    public void mostrarMensaje(String remitente, String asunto, String html) {
        if (lblRemitente != null) {
            lblRemitente.setText(remitente != null ? remitente : "");
        }
        if (lblAsunto != null) {
            lblAsunto.setText(asunto != null ? asunto : "");
        }

        if (engine != null) {
            if (html == null || html.isBlank()) {
                engine.loadContent(
                        "<html><body style='font-family: sans-serif; font-size: 13px;'><i>(Mensaje vacío)</i></body></html>",
                        "text/html"
                );
            } else {
                engine.loadContent(html, "text/html");
            }
        }
    }
}
