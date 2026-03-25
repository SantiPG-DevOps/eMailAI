package com.emailAI.controller;

import com.emailAI.dao.DAOEventosCalendario;
import com.emailAI.dao.DAOEventosCalendario.Evento;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class CalendarioController {

    // Calendario mensual
    @FXML private Label lblMesAnio;
    @FXML private GridPane gridDias;

    // Panel de eventos del día seleccionado
    @FXML private Label lblFechaSeleccionada;
    @FXML private ListView<Evento> lstEventos;
    @FXML private TextField txtNuevoEvento;

    private DAOEventosCalendario dao;
    private Evento eventoEnEdicion;

    private YearMonth mesActual;
    private LocalDate fechaSeleccionada;

    @FXML
    private void initialize() {
        try {
            dao = new DAOEventosCalendario();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error inicializando calendario", e.getMessage());
        }

        mesActual = YearMonth.now();
        fechaSeleccionada = LocalDate.now();

        // Configurar lista de eventos
        lstEventos.setCellFactory(list -> new ListCell<>() {
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

        lstEventos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onEditarEvento();
            }
        });

        pintarCalendario();
        actualizarFechaSeleccionadaLabel();
        cargarEventos(fechaSeleccionada);
    }

    // =================== Calendario mensual ===================

    private void pintarCalendario() {
        if (gridDias == null) return;

        gridDias.getChildren().clear();

        LocalDate primerDiaMes = mesActual.atDay(1);
        int diaSemanaIndex = (primerDiaMes.getDayOfWeek().getValue() + 6) % 7; // L=0...D=6
        int diasMes = mesActual.lengthOfMonth();

        String nombreMes = mesActual.getMonth().toString().toLowerCase();
        nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
        lblMesAnio.setText(nombreMes + " " + mesActual.getYear());

        int fila = 0;
        int col = diaSemanaIndex;

        for (int dia = 1; dia <= diasMes; dia++) {
            LocalDate fecha = mesActual.atDay(dia);

            Label lblDia = new Label(String.valueOf(dia));

            StackPane celda = new StackPane(lblDia);
            celda.getStyleClass().add("cal-dia");

            if (fecha.equals(LocalDate.now())) {
                celda.getStyleClass().add("cal-dia-hoy");
            }
            if (fecha.equals(fechaSeleccionada)) {
                celda.getStyleClass().add("cal-dia-seleccionado");
            }

            // que la celda rellene toda la casilla del GridPane
            celda.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            GridPane.setFillHeight(celda, true);
            GridPane.setFillWidth(celda, true);

            celda.setOnMouseClicked(e -> {
                fechaSeleccionada = fecha;
                actualizarFechaSeleccionadaLabel();
                cargarEventos(fechaSeleccionada);
                pintarCalendario(); // refrescar estilos de selección
            });

            GridPane.setRowIndex(celda, fila);
            GridPane.setColumnIndex(celda, col);
            gridDias.getChildren().add(celda);

            col++;
            if (col > 6) {
                col = 0;
                fila++;
            }
        }
    }

    private void actualizarFechaSeleccionadaLabel() {
        if (lblFechaSeleccionada != null && fechaSeleccionada != null) {
            lblFechaSeleccionada.setText("Eventos de " + fechaSeleccionada.toString());
        }
    }

    @FXML
    private void onMesAnterior() {
        mesActual = mesActual.minusMonths(1);
        pintarCalendario();
    }

    @FXML
    private void onMesSiguiente() {
        mesActual = mesActual.plusMonths(1);
        pintarCalendario();
    }

    @FXML
    private void onHoy() {
        LocalDate hoy = LocalDate.now();
        mesActual = YearMonth.from(hoy);
        fechaSeleccionada = hoy;
        pintarCalendario();
        actualizarFechaSeleccionadaLabel();
        cargarEventos(hoy);
    }

    // =================== Eventos por fecha ===================

    private void cargarEventos(LocalDate fecha) {
        if (dao == null || fecha == null) return;
        try {
            List<Evento> lista = dao.listarPorFecha(fecha);
            lstEventos.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error cargando eventos", e.getMessage());
        }
    }

    @FXML
    private void onAgregarEvento() {
        if (dao == null) return;
        String texto = txtNuevoEvento.getText().trim();
        if (texto.isEmpty() || fechaSeleccionada == null) {
            return;
        }

        try {
            if (eventoEnEdicion != null) {
                Evento actualizado = new Evento(
                        eventoEnEdicion.id(),
                        fechaSeleccionada,
                        texto,
                        eventoEnEdicion.detalle(),
                        eventoEnEdicion.origen()
                );
                dao.actualizarEvento(actualizado);
                eventoEnEdicion = null;
            } else {
                dao.guardarEvento(fechaSeleccionada, texto, null, "local");
            }

            txtNuevoEvento.clear();
            cargarEventos(fechaSeleccionada);
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
            cargarEventos(fechaSeleccionada);
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

        eventoEnEdicion = seleccionado;
        fechaSeleccionada = seleccionado.fecha();
        txtNuevoEvento.setText(seleccionado.titulo());
        actualizarFechaSeleccionadaLabel();
        pintarCalendario();
    }

    private void mostrarError(String titulo, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}