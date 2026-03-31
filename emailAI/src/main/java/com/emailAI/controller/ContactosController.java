package com.emailAI.controller;

import com.emailAI.dao.DAOContactos;
import com.emailAI.model.Contacto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

// Gestiona la agenda de contactos: listado, edición de detalle y operaciones CRUD.
public class ContactosController {

    // Lista de contactos mostrada en formato tarjeta.
    @FXML
    private ListView<Contacto> lstContactos;

    // Campo de nombre del contacto seleccionado.
    @FXML
    private TextField txtNombre;

    // Campo de email del contacto seleccionado.
    @FXML
    private TextField txtEmail;

    // Campo de teléfono del contacto seleccionado.
    @FXML
    private TextField txtTelefono;

    // Campo de notas del contacto seleccionado.
    @FXML
    private TextArea txtNotas;

    // Etiqueta de estado para validaciones y mensajes de resultado.
    @FXML
    private Label lblEstado;

    private DAOContactos daoContactos; // DAO de persistencia de contactos en SQLite.
    private final ObservableList<Contacto> modeloContactos = FXCollections.observableArrayList(); // Modelo observable enlazado a la lista.

    // Inicializa DAO, define el render de celdas y carga contactos existentes.
    @FXML
    private void initialize() {
        daoContactos = new DAOContactos("jdbc:sqlite:emailAI.db");

        lstContactos.setItems(modeloContactos);
        lstContactos.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Contacto c, boolean empty) {
                super.updateItem(c, empty);

                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("contact-card-cell");
                    return;
                }

                // Construye el contenedor visual principal de la tarjeta.
                HBox root = new HBox(10);
                root.getStyleClass().add("contact-card");
                // Deja los clics en la celda para mantener selección correcta.
                root.setPickOnBounds(false);

                // Construye la columna izquierda con nombre y email.
                VBox leftBox = new VBox(2);

                Label lblNombre = new Label(c.getNombre() != null ? c.getNombre() : "");
                lblNombre.getStyleClass().add("contact-name");

                String email = c.getEmail() != null ? c.getEmail() : "";
                Label lblEmail = new Label(email);
                lblEmail.getStyleClass().add("contact-email");

                leftBox.getChildren().add(lblNombre);
                if (!email.isBlank()) {
                    leftBox.getChildren().add(lblEmail);
                }

                // Muestra el teléfono alineado en la parte derecha.
                Label lblTelefono = new Label(c.getTelefono() != null ? c.getTelefono() : "");
                lblTelefono.getStyleClass().add("contact-phone");

                HBox.setHgrow(leftBox, Priority.ALWAYS);

                root.getChildren().addAll(leftBox, lblTelefono);

                setText(null);
                setGraphic(root);

                // Mantiene la clase CSS de tarjeta solo cuando hay datos.
                getStyleClass().removeAll("contact-card-cell");
                getStyleClass().add("contact-card-cell");
            }
        });

        lstContactos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> mostrarContacto(newSel)
        );

        cargarContactos();
    }

    // Carga todos los contactos desde BD y selecciona el primero si existe.
    private void cargarContactos() {
        List<Contacto> lista = daoContactos.listarTodos();
        modeloContactos.setAll(lista);
        if (!modeloContactos.isEmpty()) {
            lstContactos.getSelectionModel().selectFirst();
        } else {
            limpiarFormulario();
        }
    }

    // Muestra en el formulario los datos del contacto seleccionado.
    private void mostrarContacto(Contacto c) {
        if (c == null) {
            limpiarFormulario();
            return;
        }
        txtNombre.setText(c.getNombre());
        txtEmail.setText(c.getEmail());
        txtTelefono.setText(c.getTelefono());
        txtNotas.setText(c.getNotas());
        lblEstado.setText("");
    }

    // Limpia todos los campos del formulario de detalle.
    private void limpiarFormulario() {
        txtNombre.clear();
        txtEmail.clear();
        txtTelefono.clear();
        txtNotas.clear();
        lblEstado.setText("");
    }

    // Prepara el formulario para crear un contacto nuevo.
    @FXML
    private void onNuevoContacto() {
        lstContactos.getSelectionModel().clearSelection();
        limpiarFormulario();
    }

    // Guarda un contacto nuevo o actualiza el existente según la selección actual.
    @FXML
    private void onGuardarContacto() {
        String nombre = txtNombre.getText();
        if (nombre == null || nombre.isBlank()) {
            lblEstado.setText("El nombre es obligatorio.");
            return;
        }

        Contacto seleccionado = lstContactos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            // Crea y persiste un contacto nuevo.
            Contacto nuevo = new Contacto(
                    null,
                    nombre.trim(),
                    safe(txtEmail.getText()),
                    safe(txtTelefono.getText()),
                    safe(txtNotas.getText())
            );
            daoContactos.guardarOActualizar(nuevo);
            modeloContactos.add(nuevo);
            lstContactos.getSelectionModel().select(nuevo);
            lblEstado.setText("Contacto creado.");
        } else {
            // Actualiza el contacto existente seleccionado.
            seleccionado.setNombre(nombre.trim());
            seleccionado.setEmail(safe(txtEmail.getText()));
            seleccionado.setTelefono(safe(txtTelefono.getText()));
            seleccionado.setNotas(safe(txtNotas.getText()));

            daoContactos.guardarOActualizar(seleccionado);
            lstContactos.refresh();
            lblEstado.setText("Contacto actualizado.");
        }
    }

    // Borra el contacto seleccionado de la BD y del modelo de la lista.
    @FXML
    private void onBorrarContacto() {
        Contacto seleccionado = lstContactos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblEstado.setText("Selecciona un contacto para borrar.");
            return;
        }
        daoContactos.borrar(seleccionado);
        modeloContactos.remove(seleccionado);
        limpiarFormulario();
        lblEstado.setText("Contacto borrado.");
    }

    // Normaliza texto nulo y aplica trim para guardar datos limpios.
    private String safe(String s) {
        return s != null ? s.trim() : "";
    }
}