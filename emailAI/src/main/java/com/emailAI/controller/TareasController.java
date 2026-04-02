package com.emailAI.controller;

import com.emailAI.dao.DAOTareas;
import com.emailAI.model.Tarea;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TareasController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ListView<Tarea>  lstTareas;
    @FXML private ListView<String> lstDias;
    @FXML private ListView<String> lstSubtareas;
    @FXML private ListView<String> lstEtiquetas;

    @FXML private Label  lblPanelTitulo;
    @FXML private Label  lblSubtareas;
    @FXML private VBox   vboxEtiquetas;

    @FXML private TextField        txtNuevaEtiqueta;
    @FXML private ComboBox<String> cmbPrioridadEtiqueta;

    @FXML private ToggleButton btnHoy;
    @FXML private ToggleButton btnManana;
    @FXML private ToggleButton btnSemana;
    @FXML private ToggleButton btnMes;
    @FXML private ToggleButton btnPlanificado;
    @FXML private ToggleButton btnEtiquetas;

    // ── Estado interno ────────────────────────────────────────────────────────
    private DAOTareas daoTareas;
    private final ObservableList<Tarea> modeloTareas  = FXCollections.observableArrayList();
    private FilteredList<Tarea>         tareasFiltradas;

    private final Map<Integer, ObservableList<String>> subtareasMap    = new HashMap<>();
    private final Map<String, String>                  etiquetasGlobales = new LinkedHashMap<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    private void initialize() {
        daoTareas = new DAOTareas(); // ← sin URL

        tareasFiltradas = new FilteredList<>(modeloTareas, t -> true);
        lstTareas.setItems(tareasFiltradas);

        cmbPrioridadEtiqueta.getItems().setAll("ALTA", "MEDIA", "BAJA");
        cmbPrioridadEtiqueta.getSelectionModel().select("MEDIA");

        lstTareas.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Tarea t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null); setGraphic(null);
                    getStyleClass().removeAll("contact-card-cell");
                    return;
                }

                HBox root = new HBox(10);
                root.getStyleClass().add("contact-card");

                VBox left = new VBox(2);
                Label lblTitulo = new Label(t.getTitulo() != null ? t.getTitulo() : "");
                lblTitulo.getStyleClass().add("contact-name");

                Label lblEti = new Label(t.getEtiquetas() != null ? t.getEtiquetas() : "");
                lblEti.getStyleClass().add("contact-email");

                left.getChildren().add(lblTitulo);
                if (t.getEtiquetas() != null && !t.getEtiquetas().isBlank())
                    left.getChildren().add(lblEti);

                String prio = t.getPrioridad() != null ? t.getPrioridad() : "MEDIA";
                Label lblPrio = new Label(prio);
                lblPrio.getStyleClass().addAll("task-priority-badge",
                        "task-priority-" + prio.toLowerCase());

                Label lblFecha = new Label(t.getFechaVencimiento() != null
                        ? t.getFechaVencimiento().format(FMT) : "");
                lblFecha.getStyleClass().add("contact-phone");

                HBox.setHgrow(left, Priority.ALWAYS);
                root.getChildren().addAll(left, lblPrio, lblFecha);

                setText(null);
                setGraphic(root);
                getStyleClass().removeAll("contact-card-cell");
                getStyleClass().add("contact-card-cell");
            }
        });

        lstTareas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> actualizarPanelDerecho());

        cargarTareas();
    }

    // ── Carga ─────────────────────────────────────────────────────────────────
    private void cargarTareas() {
        modeloTareas.setAll(daoTareas.listarTodas());
        actualizarPanelDerecho();
    }

    // ── Panel derecho ─────────────────────────────────────────────────────────
    private void actualizarPanelDerecho() {
        boolean esEtiquetas = btnEtiquetas.isSelected();

        vboxEtiquetas.setVisible(esEtiquetas);
        vboxEtiquetas.setManaged(esEtiquetas);

        if (esEtiquetas) {
            lblPanelTitulo.setText("Etiquetas por prioridad");
            lstDias.setVisible(false);
            lstDias.setManaged(false);
            refrescarListaEtiquetas();
        } else {
            lstDias.setVisible(true);
            lstDias.setManaged(true);
            actualizarTimeline();
        }

        Tarea sel = lstTareas.getSelectionModel().getSelectedItem();
        if (sel != null) {
            lblSubtareas.setText("Subtareas: " + sel.getTitulo());
            Integer key = sel.getId() != null ? sel.getId() : System.identityHashCode(sel);
            lstSubtareas.setItems(subtareasMap.computeIfAbsent(
                    key, k -> FXCollections.observableArrayList()));
        } else {
            lblSubtareas.setText("Subtareas de la tarea seleccionada:");
            lstSubtareas.setItems(FXCollections.observableArrayList());
        }
    }

    private void actualizarTimeline() {
        if      (btnHoy.isSelected())        lblPanelTitulo.setText("Tareas para hoy");
        else if (btnManana.isSelected())     lblPanelTitulo.setText("Tareas de mañana");
        else if (btnSemana.isSelected())     lblPanelTitulo.setText("Tareas esta semana");
        else if (btnMes.isSelected())        lblPanelTitulo.setText("Tareas este mes");
        else if (btnPlanificado.isSelected())lblPanelTitulo.setText("Todas las tareas planificadas");
        else                                 lblPanelTitulo.setText("Vista general");

        List<String> lineas = new ArrayList<>();

        List<Tarea> sinFecha = new ArrayList<>();
        for (Tarea t : tareasFiltradas)
            if (t.getFechaVencimiento() == null) sinFecha.add(t);

        if (!sinFecha.isEmpty()) {
            lineas.add("📋 Sin fecha asignada (" + sinFecha.size() + ")");
            for (Tarea t : sinFecha)
                lineas.add("   • " + t.getTitulo() + prioBadge(t));
            lineas.add("");
        }

        Map<LocalDate, List<Tarea>> mapa = new TreeMap<>();
        for (Tarea t : tareasFiltradas) {
            LocalDate f = t.getFechaVencimiento();
            if (f != null) mapa.computeIfAbsent(f, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<LocalDate, List<Tarea>> e : mapa.entrySet()) {
            List<Tarea> lista = e.getValue();
            lineas.add("📅 " + e.getKey().format(FMT)
                    + "  (" + lista.size() + " tarea" + (lista.size() > 1 ? "s" : "") + ")");
            for (Tarea t : lista)
                lineas.add("   • " + t.getTitulo() + prioBadge(t));
            lineas.add("");
        }

        if (lineas.isEmpty()) lineas.add("No hay tareas para mostrar.");
        lstDias.setItems(FXCollections.observableArrayList(lineas));
    }

    private String prioBadge(Tarea t) {
        if (t.getPrioridad() == null) return "";
        return switch (t.getPrioridad()) {
            case "ALTA"  -> "  [🔴 ALTA]";
            case "MEDIA" -> "  [🟡 MEDIA]";
            case "BAJA"  -> "  [🟢 BAJA]";
            default      -> "";
        };
    }

    // ── Etiquetas ─────────────────────────────────────────────────────────────
    private void refrescarListaEtiquetas() {
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, String> e : etiquetasGlobales.entrySet())
            items.add(e.getKey() + "  →  " + e.getValue());
        if (items.isEmpty()) items.add("No hay etiquetas definidas.");
        lstEtiquetas.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    private void onAnadirEtiqueta() {
        String nombre = txtNuevaEtiqueta.getText();
        if (nombre == null || nombre.isBlank()) return;
        String prio = cmbPrioridadEtiqueta.getValue();
        if (prio == null) prio = "MEDIA";
        etiquetasGlobales.put(nombre.trim(), prio);
        txtNuevaEtiqueta.clear();
        refrescarListaEtiquetas();
    }

    // ── Subtareas ─────────────────────────────────────────────────────────────
    @FXML
    private void onAnadirSubtarea() {
        Tarea sel = lstTareas.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nueva subtarea");
        dlg.setHeaderText("Tarea: " + sel.getTitulo());
        dlg.setContentText("Descripción de la subtarea:");

        Scene mainScene = lstTareas.getScene();
        if (mainScene != null)
            dlg.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets());

        dlg.showAndWait().ifPresent(texto -> {
            if (!texto.isBlank()) {
                Integer key = sel.getId() != null ? sel.getId() : System.identityHashCode(sel);
                subtareasMap.computeIfAbsent(key, k -> FXCollections.observableArrayList())
                        .add("☐  " + texto.trim());
                actualizarPanelDerecho();
            }
        });
    }

    // ── CRUD tareas ───────────────────────────────────────────────────────────
    @FXML
    private void onNuevaTarea() {
        Tarea nueva = mostrarDialogoTarea(null);
        if (nueva != null) {
            daoTareas.guardarOActualizar(nueva);
            modeloTareas.add(nueva);
            actualizarPanelDerecho();
        }
    }

    @FXML
    private void onEditarTarea() {
        Tarea sel = lstTareas.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Tarea editada = mostrarDialogoTarea(sel);
        if (editada != null) {
            daoTareas.guardarOActualizar(editada);
            lstTareas.refresh();
            actualizarPanelDerecho();
        }
    }

    @FXML
    private void onBorrarTarea() {
        Tarea sel = lstTareas.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        daoTareas.borrar(sel);
        modeloTareas.remove(sel);
        actualizarPanelDerecho();
    }

    private Tarea mostrarDialogoTarea(Tarea original) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/tarea-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            Scene mainScene = lstTareas.getScene();
            if (mainScene != null)
                scene.getStylesheets().addAll(mainScene.getStylesheets());

            TareaDialogController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle(original == null ? "Nueva tarea" : "Editar tarea");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            ctrl.setStage(stage);
            ctrl.inicializarEstados();
            ctrl.setTarea(original);

            stage.showAndWait();
            return ctrl.isGuardado() ? ctrl.getTarea() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    @FXML private void onFiltroHoy()         { desactivarOtros(btnHoy);         actualizarFiltro(); }
    @FXML private void onFiltroManana()      { desactivarOtros(btnManana);      actualizarFiltro(); }
    @FXML private void onFiltroSemana()      { desactivarOtros(btnSemana);      actualizarFiltro(); }
    @FXML private void onFiltroMes()         { desactivarOtros(btnMes);         actualizarFiltro(); }
    @FXML private void onFiltroPlanificado() { desactivarOtros(btnPlanificado); actualizarFiltro(); }
    @FXML private void onFiltroEtiquetas()   { desactivarOtros(btnEtiquetas);   actualizarFiltro(); }

    private void desactivarOtros(ToggleButton activo) {
        for (ToggleButton b : List.of(btnHoy, btnManana, btnSemana,
                btnMes, btnPlanificado, btnEtiquetas))
            if (b != activo) b.setSelected(false);
    }

    private void actualizarFiltro() {
        boolean ninguno = !btnHoy.isSelected() && !btnManana.isSelected()
                && !btnSemana.isSelected() && !btnMes.isSelected()
                && !btnPlanificado.isSelected() && !btnEtiquetas.isSelected();
        tareasFiltradas.setPredicate(ninguno ? t -> true : this::cumpleFiltros);
        actualizarPanelDerecho();
    }

    private boolean cumpleFiltros(Tarea t) {
        LocalDate f   = t.getFechaVencimiento();
        LocalDate hoy = LocalDate.now();
        if (btnHoy.isSelected())         return f != null && f.equals(hoy);
        if (btnManana.isSelected())      return f != null && f.equals(hoy.plusDays(1));
        if (btnSemana.isSelected())      return f != null && !f.isBefore(hoy) && !f.isAfter(hoy.plusDays(7));
        if (btnMes.isSelected())         return f != null && f.getYear() == hoy.getYear()
                                                          && f.getMonth() == hoy.getMonth();
        if (btnPlanificado.isSelected()) return f != null;
        if (btnEtiquetas.isSelected()) {
            String et = t.getEtiquetas();
            return et != null && !et.isBlank();
        }
        return true;
    }
}