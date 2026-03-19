package com.emailAI.controller;

import com.emailAI.dao.DAOEventosCalendario;
import com.emailAI.dao.DAOEventosCalendario.Evento;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;

public class CalendarioController {

    @FXML private DatePicker dpFecha;
    @FXML private ListView<Evento> lstEventos;
    @FXML private TextField txtNuevoEvento;

    private DAOEventosCalendario dao;

    // Para saber si estamos editando un evento existente
    private Evento eventoEnEdicion;

    @FXML
    private void initialize() {
        try {
            dao = new DAOEventosCalendario();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error inicializando calendario", e.getMessage());
        }

        LocalDate hoy = LocalDate.now();
        dpFecha.setValue(hoy);
        dpFecha.valueProperty().addListener((obs, old, neu) -> {
            if (neu != null) {
                cargarEventos(neu);
            }
        });

        // Mostrar título del evento en la lista
        lstEventos.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Evento ev, boolean empty) {
                super.updateItem(ev, empty);
                if (empty || ev == null) {
                    setText(null);
                } else {
                    setText(ev.titulo());
                }
            }
        });

        // Doble clic sobre un evento para ponerlo en edición
        lstEventos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onEditarEvento();
            }
        });

        cargarEventos(hoy);
    }

    private void cargarEventos(LocalDate fecha) {
        if (dao == null) return;
        try {
            List<Evento> lista = dao.listarPorFecha(fecha);
            lstEventos.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error cargando eventos", e.getMessage());
        }
    }

    @FXML
    private void onHoy() {
        LocalDate hoy = LocalDate.now();
        dpFecha.setValue(hoy);
        cargarEventos(hoy);
    }

    @FXML
    private void onAgregarEvento() {
        if (dao == null) return;
        String texto = txtNuevoEvento.getText().trim();
        LocalDate fecha = dpFecha.getValue();
        if (texto.isEmpty() || fecha == null) {
            return;
        }

        try {
            // Si hay evento en edición, actualizamos; si no, creamos nuevo
            if (eventoEnEdicion != null) {
                Evento actualizado = new Evento(
                        eventoEnEdicion.id(),
                        fecha,
                        texto,
                        eventoEnEdicion.detalle(),
                        eventoEnEdicion.origen()
                );
                dao.actualizarEvento(actualizado);
                eventoEnEdicion = null;
            } else {
                dao.guardarEvento(fecha, texto, null, "local");
            }

            txtNuevoEvento.clear();
            cargarEventos(fecha);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error guardando evento", e.getMessage());
        }
    }

    @FXML
    private void onEliminarEventoSeleccionado() {
        if (dao == null) return;
        Evento seleccionado = lstEventos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        try {
            dao.borrarEvento(seleccionado.id());
            if (eventoEnEdicion != null && eventoEnEdicion.id() == seleccionado.id()) {
                eventoEnEdicion = null;
                txtNuevoEvento.clear();
            }
            cargarEventos(dpFecha.getValue());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error borrando evento", e.getMessage());
        }
    }

    @FXML
    private void onEditarEvento() {
        Evento seleccionado = lstEventos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        // Ponemos sus datos en los controles para editar
        eventoEnEdicion = seleccionado;
        dpFecha.setValue(seleccionado.fecha());
        txtNuevoEvento.setText(seleccionado.titulo());
    }

    private void mostrarError(String titulo, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}
