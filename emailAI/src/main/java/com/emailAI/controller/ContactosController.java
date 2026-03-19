package com.emailAI.controller;

import com.emailAI.dao.DAOContactos;
import com.emailAI.dao.DAOContactos.Contacto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class ContactosController {

    @FXML private ListView<Contacto> lstContactos;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefono;
    @FXML private TextArea txtNotas;

    private DAOContactos dao;
    private int cuentaIdActual;

    @FXML
    private void initialize() {
        lstContactos.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Contacto c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    setText(c.getNombre() + "  <" + c.getEmail() + ">");
                }
            }
        });

        lstContactos.getSelectionModel().selectedItemProperty().addListener((obs, old, neu) -> {
            if (neu != null) {
                txtNombre.setText(neu.getNombre());
                txtEmail.setText(neu.getEmail());
                txtTelefono.setText(neu.getTelefono());
                txtNotas.setText(neu.getNotas());
            }
        });
    }

    // Lo llamará MainController después de crear el loader
    public void configurar(DAOContactos dao, int cuentaId) {
        this.dao = dao;
        this.cuentaIdActual = cuentaId;
        recargarContactos();
    }

    private void recargarContactos() {
        if (dao == null) return;
        try {
            List<Contacto> lista = dao.listarPorCuenta(cuentaIdActual);
            lstContactos.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error cargando contactos", e.getMessage());
        }
    }

    @FXML
    private void onAgregarContacto() {
        if (dao == null) return;
        String nombre = txtNombre.getText().trim();
        String email = txtEmail.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String notas = txtNotas.getText().trim();

        if (nombre.isEmpty() || email.isEmpty()) {
            mostrarError("Datos incompletos", "Nombre y email son obligatorios.");
            return;
        }

        try {
            dao.insertarContacto(cuentaIdActual, nombre, email, telefono, notas);
            recargarContactos();
            txtNombre.clear();
            txtEmail.clear();
            txtTelefono.clear();
            txtNotas.clear();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error guardando contacto", e.getMessage());
        }
    }

    @FXML
    private void onEliminarContacto() {
        if (dao == null) return;
        Contacto seleccionado = lstContactos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        try {
            dao.borrarContacto(seleccionado.getId());
            recargarContactos();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error eliminando contacto", e.getMessage());
        }
    }

    private void mostrarError(String titulo, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}
