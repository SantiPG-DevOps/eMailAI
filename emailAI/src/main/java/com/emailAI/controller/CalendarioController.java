package com.emailAI.controller;

import com.emailAI.dao.DAOEventosCalendario;
import com.emailAI.dao.DAOEventosCalendario.Evento;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
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

    @FXML
    private void initialize() {
        try {
            dao = new DAOEventosCalendario();
        } catch (Exception e) {
            e.printStackTrace();
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

        cargarEventos(hoy);
    }

    private void cargarEventos(LocalDate fecha) {
        if (dao == null) return;
        try {
            List<Evento> lista = dao.listarPorFecha(fecha);
            lstEventos.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
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
            dao.guardarEvento(fecha, texto, null, "local");
            txtNuevoEvento.clear();
            cargarEventos(fecha);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEliminarEventoSeleccionado() {
        if (dao == null) return;
        Evento seleccionado = lstEventos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        try {
            dao.borrarEvento(seleccionado.id());
            cargarEventos(dpFecha.getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
