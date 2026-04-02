package com.emailAI.controller;

import com.emailAI.AppFX;
import com.emailAI.dao.DAOEventosCalendario;
import com.emailAI.dao.DAOEventosCalendario.Evento;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class CalendarioController {

    @FXML private Label lblMesAnio;
    @FXML private GridPane gridDias;
    @FXML private Button btnEditarEvento;
    @FXML private Button btnBorrarEvento;

    // Estos campos son opcionales (no están en el FXML actual, se dejan por compatibilidad)
    @FXML private Label lblFechaSeleccionada;
    @FXML private ListView<Evento> lstEventos;
    @FXML private TextField txtNuevoEvento;

    private DAOEventosCalendario dao;
    private Evento eventoSeleccionado; // evento actualmente seleccionado en el calendario
    private YearMonth mesActual;
    private LocalDate fechaSeleccionada;
    private Timeline timelineRefresco;

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

        if (lstEventos != null) {
            lstEventos.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Evento ev, boolean empty) {
                    super.updateItem(ev, empty);
                    setText(empty || ev == null ? null : ev.titulo());
                }
            });
            lstEventos.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) onEditarEventoSeleccionado();
            });
        }

        pintarCalendario();
        actualizarFechaSeleccionadaLabel();
        cargarEventos(fechaSeleccionada);
        actualizarBotonesAccion();

        timelineRefresco = new Timeline(
                new KeyFrame(Duration.minutes(5), ev -> Platform.runLater(this::pintarCalendario)));
        timelineRefresco.setCycleCount(Timeline.INDEFINITE);
        timelineRefresco.play();
    }

    // Activa o desactiva los botones Editar/Borrar según si hay evento seleccionado
    private void actualizarBotonesAccion() {
        boolean haySeleccion = eventoSeleccionado != null;
        if (btnEditarEvento != null) btnEditarEvento.setDisable(!haySeleccion);
        if (btnBorrarEvento != null) btnBorrarEvento.setDisable(!haySeleccion);
    }

    private void pintarCalendario() {
        if (gridDias == null) return;

        gridDias.getChildren().clear();

        LocalDate hoy = LocalDate.now();
        Set<LocalDate> diasConEvento = Set.of();
        if (dao != null) {
            try {
                diasConEvento = dao.fechasConEventosEnRango(
                        mesActual.atDay(1), mesActual.atEndOfMonth());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LocalDate primerDiaMes = mesActual.atDay(1);
        int diaSemanaIndex = (primerDiaMes.getDayOfWeek().getValue() + 6) % 7;
        int diasMes = mesActual.lengthOfMonth();

        String nombreMes = mesActual.getMonth().toString().toLowerCase();
        nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
        if (lblMesAnio != null) lblMesAnio.setText(nombreMes + " " + mesActual.getYear());

        int fila = 0;
        int col = diaSemanaIndex;

        for (int dia = 1; dia <= diasMes; dia++) {
            LocalDate fecha = mesActual.atDay(dia);

            Label lblDia = new Label(String.valueOf(dia));
            lblDia.getStyleClass().add("cal-dia-numero");

            VBox celdaBox = new VBox(2);
            celdaBox.getChildren().add(lblDia);

            // Cargar y pintar eventos del día
            List<Evento> eventosDelDia = List.of();
            if (dao != null) {
                try { eventosDelDia = dao.listarPorFecha(fecha); }
                catch (Exception ignored) {}
            }

            for (Evento ev : eventosDelDia) {
                String horaStr = ev.hora() != null
                        ? String.format("%02d:%02d ", ev.hora().getHour(), ev.hora().getMinute())
                        : "";
                Label barraEvento = new Label(horaStr + ev.titulo());
                barraEvento.getStyleClass().add("cal-evento-barra");
                barraEvento.setMaxWidth(Double.MAX_VALUE);
                barraEvento.setEllipsisString("…");

                // Resalta si es el evento seleccionado
                if (eventoSeleccionado != null && ev.id() == eventoSeleccionado.id()) {
                    barraEvento.getStyleClass().add("cal-evento-barra-seleccionado");
                }

                // Click en barra → selecciona el evento
                final Evento evFinal = ev;
                barraEvento.setOnMouseClicked(e -> {
                    e.consume(); // no propaga a la celda
                    eventoSeleccionado = evFinal;
                    fechaSeleccionada = evFinal.fecha();
                    actualizarFechaSeleccionadaLabel();
                    cargarEventos(fechaSeleccionada);
                    actualizarBotonesAccion();
                    pintarCalendario();
                });

                // Menú contextual click derecho
                ContextMenu menu = new ContextMenu();
                MenuItem itemEditar = new MenuItem("✏ Editar");
                MenuItem itemBorrar = new MenuItem("🗑 Borrar");
                itemEditar.setOnAction(e -> {
                    eventoSeleccionado = evFinal;
                    actualizarBotonesAccion();
                    onEditarEventoSeleccionado();
                });
                itemBorrar.setOnAction(e -> {
                    eventoSeleccionado = evFinal;
                    actualizarBotonesAccion();
                    onBorrarEventoSeleccionado();
                });
                menu.getItems().addAll(itemEditar, itemBorrar);
                barraEvento.setContextMenu(menu);

                celdaBox.getChildren().add(barraEvento);
            }

            StackPane celda = new StackPane(celdaBox);
            StackPane.setAlignment(celdaBox, javafx.geometry.Pos.TOP_LEFT);
            celda.getStyleClass().add("cal-dia");

            if (fecha.equals(hoy)) celda.getStyleClass().add("cal-dia-hoy");
            if (fecha.equals(fechaSeleccionada)) celda.getStyleClass().add("cal-dia-seleccionado");

            if (diasConEvento.contains(fecha)) {
                long semanas = ChronoUnit.WEEKS.between(
                        hoy.with(DayOfWeek.MONDAY),
                        fecha.with(DayOfWeek.MONDAY));
                if (semanas == 0) celda.getStyleClass().add("cal-dia-urgente-semana");
                else if (semanas == 1) celda.getStyleClass().add("cal-dia-proxima-semana");
                else celda.getStyleClass().add("cal-dia-mas-tarde");
            }

            celda.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            GridPane.setFillHeight(celda, true);
            GridPane.setFillWidth(celda, true);

            celda.setOnMouseClicked(e -> {
                if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
                eventoSeleccionado = null; // deselecciona evento al hacer click en celda vacía
                fechaSeleccionada = fecha;
                actualizarFechaSeleccionadaLabel();
                cargarEventos(fechaSeleccionada);
                actualizarBotonesAccion();
                pintarCalendario();
            });

            GridPane.setRowIndex(celda, fila);
            GridPane.setColumnIndex(celda, col);
            gridDias.getChildren().add(celda);

            col++;
            if (col > 6) { col = 0; fila++; }
        }
    }

    private void actualizarFechaSeleccionadaLabel() {
        if (lblFechaSeleccionada != null && fechaSeleccionada != null)
            lblFechaSeleccionada.setText("Eventos de " + fechaSeleccionada.toString());
    }

    @FXML private void onMesAnterior() { mesActual = mesActual.minusMonths(1); pintarCalendario(); }
    @FXML private void onMesSiguiente() { mesActual = mesActual.plusMonths(1); pintarCalendario(); }

    @FXML
    private void onHoy() {
        LocalDate hoy = LocalDate.now();
        mesActual = YearMonth.from(hoy);
        fechaSeleccionada = hoy;
        pintarCalendario();
        actualizarFechaSeleccionadaLabel();
        cargarEventos(hoy);
    }

    @FXML
    private void onNuevoEvento() {
        if (dao == null || gridDias == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/evento-calendario-dialog.fxml"));
            Parent root = loader.load();
            EventoCalendarioDialogController dlgCtl = loader.getController();
            dlgCtl.setFechaInicial(fechaSeleccionada != null ? fechaSeleccionada : LocalDate.now());

            Dialog<ButtonType> dialog = crearDialog("Nuevo evento", root);
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okBtn != null) {
                okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
                    if (dlgCtl.getFecha() == null || dlgCtl.getTitulo().isBlank()) {
                        ev.consume();
                        mostrarError("Datos incompletos", "Indica fecha y título.");
                    }
                });
            }

            var res = dialog.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            LocalDate f = dlgCtl.getFecha();
            dao.guardarEvento(f, dlgCtl.getHora(), dlgCtl.getTitulo(), dlgCtl.getDetalle(), "local");
            mesActual = YearMonth.from(f);
            fechaSeleccionada = f;
            actualizarFechaSeleccionadaLabel();
            pintarCalendario();
            cargarEventos(f);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo crear el evento", e.getMessage());
        }
    }

    @FXML
    private void onEditarEventoSeleccionado() {
        if (eventoSeleccionado == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/evento-calendario-dialog.fxml"));
            Parent root = loader.load();
            EventoCalendarioDialogController dlgCtl = loader.getController();

            // Rellena el diálogo con los datos del evento
            dlgCtl.setFechaInicial(eventoSeleccionado.fecha());
            dlgCtl.setHoraInicial(eventoSeleccionado.hora());
            dlgCtl.setTituloInicial(eventoSeleccionado.titulo());
            dlgCtl.setDetalleInicial(eventoSeleccionado.detalle());

            Dialog<ButtonType> dialog = crearDialog("Editar evento", root);
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okBtn != null) {
                okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
                    if (dlgCtl.getFecha() == null || dlgCtl.getTitulo().isBlank()) {
                        ev.consume();
                        mostrarError("Datos incompletos", "Indica fecha y título.");
                    }
                });
            }

            var res = dialog.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            Evento actualizado = new Evento(
                    eventoSeleccionado.id(),
                    dlgCtl.getFecha(),
                    dlgCtl.getHora(),
                    dlgCtl.getTitulo(),
                    dlgCtl.getDetalle(),
                    eventoSeleccionado.origen()
            );
            dao.actualizarEvento(actualizado);
            eventoSeleccionado = null;
            actualizarBotonesAccion();
            mesActual = YearMonth.from(actualizado.fecha());
            fechaSeleccionada = actualizado.fecha();
            actualizarFechaSeleccionadaLabel();
            pintarCalendario();
            cargarEventos(fechaSeleccionada);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo editar el evento", e.getMessage());
        }
    }

    @FXML
    private void onBorrarEventoSeleccionado() {
        if (eventoSeleccionado == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Borrar evento");
        confirm.setHeaderText("¿Borrar \"" + eventoSeleccionado.titulo() + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            dao.borrarEvento(eventoSeleccionado.id());
            eventoSeleccionado = null;
            actualizarBotonesAccion();
            pintarCalendario();
            cargarEventos(fechaSeleccionada);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error borrando evento", e.getMessage());
        }
    }

    private Dialog<ButtonType> crearDialog(String titulo, Parent root) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.initModality(Modality.WINDOW_MODAL);
        Window w = gridDias.getScene() != null ? gridDias.getScene().getWindow() : null;
        if (w != null) dialog.initOwner(w);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        Scene parentScene = gridDias.getScene();
        if (parentScene != null)
            dialog.getDialogPane().getStylesheets().setAll(parentScene.getStylesheets());
        return dialog;
    }

    private void cargarEventos(LocalDate fecha) {
        if (dao == null || fecha == null || lstEventos == null) return;
        try {
            lstEventos.getItems().setAll(dao.listarPorFecha(fecha));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Métodos legacy mantenidos por compatibilidad
    @FXML private void onAgregarEvento() { onNuevoEvento(); }
    @FXML private void onEliminarEventoSeleccionado() { onBorrarEventoSeleccionado(); }
    @FXML private void onEditarEvento() { onEditarEventoSeleccionado(); }

    private void mostrarError(String titulo, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}