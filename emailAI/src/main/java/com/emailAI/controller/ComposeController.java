package com.emailAI.controller;

import com.emailAI.model.Mensaje;
import com.emailAI.service.MailService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Controlador de la ventana de redacción de correos (nuevo, responder, reenviar).
public class ComposeController {

    @FXML private TextField txtPara;   // Campo de destinatario principal (TO).
    @FXML private TextField txtCc;     // Campo de copia (CC), de momento no usado por MailService.
    @FXML private TextField txtAsunto; // Campo de asunto del correo.
    @FXML private TextArea txtCuerpo;  // Cuerpo de texto del correo.
    @FXML private Label lblEstado;     // Etiqueta para mostrar mensajes de error/éxito al usuario.

    private MailService mailService;   // Servicio responsable de enviar el correo.
    private Stage stage;               // Ventana que contiene la vista de composición.
    private Mensaje mensajeOrigen;     // Mensaje base cuando se responde o reenvía.

    // Permite inyectar el MailService que ya está conectado.
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    // Asigna el Stage para poder cerrar la ventana al enviar o cancelar.
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // Inicializa el formulario para redactar un correo completamente nuevo.
    public void inicializarNuevo() {
        setMensajeOrigen(null);
        txtPara.clear();
        txtCc.clear();
        txtAsunto.clear();
        txtCuerpo.clear();
        lblEstado.setText("");
    }

    // Prepara la vista para responder solo al remitente del mensaje original.
    public void inicializarResponder(Mensaje origen) {
        setMensajeOrigen(origen);
        txtPara.setText(origen.getRemitente());
        txtCc.clear();
        txtAsunto.setText("Re: " + safe(origen.getAsunto()));
        txtCuerpo.setText("\n\n----- Mensaje original -----\n" + safe(origen.getCuerpo()));
        lblEstado.setText("");
    }

    // Prepara la vista para responder a todos (placeholder actual en CC).
    public void inicializarResponderTodos(Mensaje origen) {
        setMensajeOrigen(origen);
        txtPara.setText(origen.getRemitente());
        txtCc.clear(); // Más adelante: añadir resto de destinatarios
        txtAsunto.setText("Re: " + safe(origen.getAsunto()));
        txtCuerpo.setText("\n\n----- Mensaje original -----\n" + safe(origen.getCuerpo()));
        lblEstado.setText("");
    }

    // Prepara la vista para reenviar un mensaje, incluyendo encabezados básicos.
    public void inicializarReenviar(Mensaje origen) {
        setMensajeOrigen(origen);
        txtPara.clear();
        txtCc.clear();
        txtAsunto.setText("Fwd: " + safe(origen.getAsunto()));
        txtCuerpo.setText(
                "\n\n----- Mensaje reenviado -----\n" +
                "De: " + safe(origen.getRemitente()) + "\n" +
                "Asunto: " + safe(origen.getAsunto()) + "\n\n" +
                safe(origen.getCuerpo())
        );
        lblEstado.setText("");
    }

    // Valida los campos y delega en MailService el envío del correo.
    @FXML
    private void onEnviar() {
        if (mailService == null) {
            lblEstado.setText("Error: MailService no disponible.");
            return;
        }

        String to = txtPara.getText() != null ? txtPara.getText().trim() : "";
        String subject = txtAsunto.getText() != null ? txtAsunto.getText().trim() : "";
        String body = txtCuerpo.getText() != null ? txtCuerpo.getText() : "";

        if (to.isEmpty()) {
            lblEstado.setText("El campo 'Para' es obligatorio.");
            return;
        }

        if (subject.isEmpty()) {
            lblEstado.setText("El campo 'Asunto' es obligatorio.");
            return;
        }

        try {
            // Envía por TO; CC/BCC quedan para una ampliación del servicio.
            mailService.sendEmail(to, subject, body);
            lblEstado.setText("Correo enviado correctamente.");

            if (stage == null && txtPara.getScene() != null) {
                stage = (Stage) txtPara.getScene().getWindow();
            }
            if (stage != null) {
                stage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("Error al enviar: " + e.getMessage());
        }
    }

    // Cierra la ventana de composición sin enviar el mensaje.
    @FXML
    private void onCancelar() {
        if (stage == null && txtPara.getScene() != null) {
            stage = (Stage) txtPara.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    // Devuelve una cadena no nula para evitar NPE en concatenaciones.
    private String safe(String s) {
        return s != null ? s : "";
    }

	public Mensaje getMensajeOrigen() {
		return mensajeOrigen;
	}

	public void setMensajeOrigen(Mensaje mensajeOrigen) {
		this.mensajeOrigen = mensajeOrigen;
	}
	
	public void setCuerpoInicial(String texto) {
	    if (txtCuerpo != null) {
	        txtCuerpo.setText(texto != null ? texto : "");
	        txtCuerpo.requestFocus();
	        txtCuerpo.positionCaret(txtCuerpo.getText().length());
	    }
	}
}