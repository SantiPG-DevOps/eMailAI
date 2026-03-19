package com.emailAI.controller;

import com.emailAI.dao.DAOEventosCalendario;
import com.emailAI.dao.DAOEventosCalendario.Evento;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarioController {

    @FXML private GridPane gridCalendario;
    @FXML private Label lblMesAnio;
    @FXML private TextField txtNuevoEvento;

    private DAOEventosCalendario dao;

    // mes actualmente mostrado
    private YearMonth mesActual;

    // día seleccionado para añadir evento
    private LocalDate diaSeleccionado;

    // mapa celda -> fecha
    private final Map<VBox, LocalDate> mapaCeldas = new HashMap<>();

    @FXML
    private void initialize() {
        try {
            dao = new DAOEventosCalendario();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error inicializando calendario", e.getMessage());
        }

        mesActual = YearMonth.now();
        construirCalendario();
    }

    private void construirCalendario() {
        gridCalendario.getChildren().clear();
        mapaCeldas.clear();

        lblMesAnio.setText(mesActual.getMonth().toString() + " " + mesActual.getYear());

        LocalDate primerDiaMes = mesActual.atDay(1);
        // ajustar para que la semana empiece en lunes
        int offsetColumna = (primerDiaMes.getDayOfWeek().getValue() + 6) % 7; // L=1 ->0, ... D=7->6

        LocalDate fechaIter = primerDiaMes;

        for (int dia = 1; dia <= mesActual.lengthOfMonth(); dia++) {
            int index = offsetColumna + (dia - 1);
            int fila = index / 7;
            int col = index % 7;

            VBox celda = crearCeldaDia(fechaIter);
            gridCalendario.add(celda, col, fila);

            fechaIter = fechaIter.plusDays(1);
        }
    }

    private VBox crearCeldaDia(LocalDate fecha) {
        VBox caja = new VBox(4);
        caja.setPadding(new Insets(4));
        caja.getStyleClass().add("calendario-dia");

        Label lblNumero = new Label(String.valueOf(fecha.getDayOfMonth()));
        lblNumero.getStyleClass().add("calendario-dia-numero");

        Label lblEventos = new Label();
        lblEventos.getStyleClass().add("calendario-dia-eventos");

        caja.getChildren().addAll(lblNumero, lblEventos);
        mapaCeldas.put(caja, fecha);

        // marcar hoy
        if (fecha.equals(LocalDate.now())) {
            caja.getStyleClass().add("calendario-hoy");
        }

        // cargar eventos de ese día
        actualizarLabelEventos(fecha, lblEventos);

        // click para seleccionar día
        caja.setOnMouseClicked(ev -> {
            diaSeleccionado = fecha;
            resaltarSeleccion(caja);
        });

        return caja;
    }

    private void actualizarLabelEventos(LocalDate fecha, Label lblEventos) {
        if (dao == null) {
            lblEventos.setText("");
            return;
        }
        try {
            List<Evento> lista = dao.listarPorFecha(fecha);
            if (lista.isEmpty()) {
                lblEventos.setText("");
            } else if (lista.size() <= 2) {
                StringBuilder sb = new StringBuilder();
                for (Evento e : lista) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append("• ").append(e.titulo());
                }
                lblEventos.setText(sb.toString());
            } else {
                lblEventos.setText(lista.size() + " eventos");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblEventos.setText("");
        }
    }

    private void resaltarSeleccion(VBox seleccionada) {
        // limpiar selección anterior
        gridCalendario.getChildren().forEach(node -> node.getStyleClass().remove("calendario-seleccionado"));
        seleccionada.getStyleClass().add("calendario-seleccionado");
    }

    @FXML
    private void onHoy() {
        mesActual = YearMonth.now();
        construirCalendario();
        diaSeleccionado = LocalDate.now();
        // resaltar hoy
        gridCalendario.getChildren().forEach(node -> {
            if (node instanceof VBox v && mapaCeldas.get(v) != null && mapaCeldas.get(v).equals(diaSeleccionado)) {
                resaltarSeleccion(v);
            }
        });
    }

    @FXML
    private void onMesAnterior() {
        mesActual = mesActual.minusMonths(1);
        construirCalendario();
    }

    @FXML
    private void onMesSiguiente() {
        mesActual = mesActual.plusMonths(1);
        construirCalendario();
    }

    @FXML
    private void onAgregarEvento() {
        if (dao == null) return;
        String texto = txtNuevoEvento.getText().trim();
        if (texto.isEmpty()) return;

        LocalDate fecha = diaSeleccionado != null ? diaSeleccionado : LocalDate.now();

        try {
            dao.guardarEvento(fecha, texto, null, "local");
            txtNuevoEvento.clear();
            // recargar solo la celda de ese día
            gridCalendario.getChildren().forEach(node -> {
                if (node instanceof VBox v && fecha.equals(mapaCeldas.get(v))) {
                    Label lblEventos = (Label) v.getChildren().get(1);
                    actualizarLabelEventos(fecha, lblEventos);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error guardando evento", e.getMessage());
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
