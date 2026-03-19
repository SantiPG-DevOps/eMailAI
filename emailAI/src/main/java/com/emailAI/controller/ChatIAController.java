package com.emailAI.controller;

import com.emailAI.service.IAAsistenteService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatIAController {

    @FXML private TextArea txtHistorial;
    @FXML private TextField txtEntrada;

    private IAAsistenteService iaService;

    public ChatIAController() {
        // constructor vacío, el FXML lo usa sin problemas
    }

    @FXML
    private void initialize() {
        appendLinea("IA", "Hola, soy tu asistente para el correo. ¿En qué te ayudo?");
    }

    // Lo llamará MainController después de cargar el FXML
    public void setIaService(IAAsistenteService iaService) {
        this.iaService = iaService;
    }

    @FXML
    private void onEnviar() {
        String mensaje = txtEntrada.getText().trim();
        if (mensaje.isEmpty() || iaService == null) {
            return;
        }

        appendLinea("Tú", mensaje);
        txtEntrada.clear();

        new Thread(() -> {
            String respuesta;
            try {
                respuesta = iaService.chatear(mensaje);
            } catch (Exception e) {
                e.printStackTrace();
                respuesta = "Ha ocurrido un error al consultar la IA.";
            }
            String finalRespuesta = respuesta;
            Platform.runLater(() -> appendLinea("IA", finalRespuesta));
        }).start();
    }

    private void appendLinea(String quien, String texto) {
        if (txtHistorial.getText().isEmpty()) {
            txtHistorial.appendText(quien + ": " + texto);
        } else {
            txtHistorial.appendText("\n\n" + quien + ": " + texto);
        }
        txtHistorial.setScrollTop(Double.MAX_VALUE);
    }
}
