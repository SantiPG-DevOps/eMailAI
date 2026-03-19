package com.emailAI.controller;

import com.emailAI.dao.DAOTareas;
import com.emailAI.dao.DAOTareas.Tarea;
import com.emailAI.dao.DAOTareas.Estado;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.List;

public class TareasController {

    @FXML private ListView<Tarea> lstTareas;
    @FXML private TextField txtNuevaTarea;
    @FXML private DatePicker dpFechaLimite;

    private DAOTareas dao;
    private int cuentaIdActual;

    @FXML
    private void initialize() {
        lstTareas.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Tarea tarea, boolean empty) {
                super.updateItem(tarea, empty);
                if (empty || tarea == null) {
                    setText(null);
                } else {
                    StringBuilder sb = new StringBuilder();
                    if (tarea.estaCompletada()) {
                        sb.append("[✔] ");
                    } else {
                        sb.append("[ ] ");
                    }
                    sb.append(tarea.getTitulo());
                    if (tarea.getFechaLimite() != null) {
                        sb.append("  (")
                          .append(tarea.getFechaLimite())
                          .append(")");
                    }
                    setText(sb.toString());
                }
            }
        });
    }

    // Lo llama MainController después de cargar el FXML
    public void configurar(DAOTareas dao, int cuentaId) {
        this.dao = dao;
        this.cuentaIdActual = cuentaId;
        recargarTareas();
    }

    private void recargarTareas() {
        if (dao == null) return;
        try {
            List<Tarea> lista = dao.listarPorCuenta(cuentaIdActual);
            lstTareas.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error cargando tareas", e.getMessage());
        }
    }

    @FXML
    private void onAgregarTarea() {
        if (dao == null) return;
        String titulo = txtNuevaTarea.getText().trim();
        LocalDate fecha = dpFechaLimite.getValue();

        if (titulo.isEmpty()) {
            mostrarError("Tarea vacía", "Escribe un título para la tarea.");
            return;
        }

        try {
            dao.insertarTarea(cuentaIdActual, titulo, null, fecha);
            txtNuevaTarea.clear();
            dpFechaLimite.setValue(null);
            recargarTareas();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error guardando tarea", e.getMessage());
        }
    }

    @FXML
    private void onCompletarTarea() {
        if (dao == null) return;
        Tarea seleccionada = lstTareas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;

        try {
            Estado nuevo = seleccionada.estaCompletada() ? Estado.PENDIENTE : Estado.COMPLETADA;
            dao.actualizarEstado(seleccionada.getId(), nuevo);
            recargarTareas();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error actualizando tarea", e.getMessage());
        }
    }

    @FXML
    private void onEliminarTarea() {
        if (dao == null) return;
        Tarea seleccionada = lstTareas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;

        try {
            dao.borrarTarea(seleccionada.getId());
            recargarTareas();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error eliminando tarea", e.getMessage());
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
