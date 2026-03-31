package com.emailAI;

import com.emailAI.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Punto de entrada JavaFX que arranca la app y mantiene una referencia global al MainController.
public class AppFX extends Application {

    private static MainController mainController; // Referencia estática al controlador principal para acceso desde otras vistas.

    // Carga la vista de login, aplica estilos iniciales y muestra la ventana principal.
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/login-view.fxml"));
        Scene scene = new Scene(loader.load(), 800, 650);

        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-basic.css").toExternalForm()
        );
        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-dark.css").toExternalForm()
        );

        stage.setTitle("Cliente de Correo IA - Login");
        stage.setScene(scene);
        stage.show();
    }

    // Permite registrar el MainController justo después de cargar la vista principal.
    public static void setMainController(MainController controller) {
        mainController = controller;
    }

    // Devuelve el MainController si ya ha sido inicializado.
    public static MainController getMainController() {
        return mainController;
    }

    // Método main estándar que lanza la aplicación JavaFX.
    public static void main(String[] args) {
        launch(args);
    }
}