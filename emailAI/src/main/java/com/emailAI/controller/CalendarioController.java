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

// Gestiona la vista de calendario mensual y los eventos asociados al día seleccionado.
public class CalendarioController {

    // Declara componentes visuales de la rejilla mensual.
    @FXML private Label lblMesAnio; // Etiqueta que muestra el mes y año en curso.
    @FXML private GridPane gridDias; // Rejilla donde se dibujan los días del mes.

    // Declara componentes del panel de eventos para la fecha activa.
    @FXML private Label lblFechaSeleccionada; // Cabecera de fecha del panel de eventos.
    @FXML private ListView<Evento> lstEventos; // Lista de eventos para la fecha seleccionada.
    @FXML private TextField txtNuevoEvento; // Campo para crear o editar título de evento.

    private DAOEventosCalendario dao; // DAO de persistencia de eventos de calendario.
    private Evento eventoEnEdicion; // Evento temporalmente seleccionado para edición.

    private YearMonth mesActual; // Mes visible actualmente en el calendario.
    private LocalDate fechaSeleccionada; // Día activo para cargar/editar eventos.

    // Inicializa calendario, listeners de UI y primera carga de eventos.
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

        // Configura cómo se visualiza cada evento en la lista.
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

    // Dibuja visualmente todas las celdas del mes actual en el GridPane.
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

            // Fuerza que la celda ocupe toda su posición del GridPane.
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

    // Actualiza el texto de cabecera con la fecha actualmente seleccionada.
    private void actualizarFechaSeleccionadaLabel() {
        if (lblFechaSeleccionada != null && fechaSeleccionada != null) {
            lblFechaSeleccionada.setText("Eventos de " + fechaSeleccionada.toString());
        }
    }

    // Navega al mes anterior y repinta el calendario.
    @FXML
    private void onMesAnterior() {
        mesActual = mesActual.minusMonths(1);
        pintarCalendario();
    }

    // Navega al mes siguiente y repinta el calendario.
    @FXML
    private void onMesSiguiente() {
        mesActual = mesActual.plusMonths(1);
        pintarCalendario();
    }

    // Vuelve a la fecha actual del sistema y recarga sus eventos.
    @FXML
    private void onHoy() {
        LocalDate hoy = LocalDate.now();
        mesActual = YearMonth.from(hoy);
        fechaSeleccionada = hoy;
        pintarCalendario();
        actualizarFechaSeleccionadaLabel();
        cargarEventos(hoy);
    }

    // Carga de BD los eventos del día indicado y los muestra en la lista.
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

    // Crea un evento nuevo o actualiza el evento en edición.
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

    // Elimina el evento seleccionado y limpia estado de edición si aplica.
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

    // Pone en modo edición el evento seleccionado tras doble click.
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

    // Muestra un Alert de error con título y detalle.
    private void mostrarError(String titulo, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}