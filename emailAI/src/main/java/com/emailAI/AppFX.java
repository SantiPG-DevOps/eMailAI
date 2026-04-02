package com.emailAI;

import com.emailAI.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppFX extends Application {

    private static MainController mainController;
    private static Scene mainScene;

    private static final String CSS_BASIC = "/styles-basic.css";
    private static final String CSS_DARK  = "/styles-dark.css";

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/login-view.fxml"));
        mainScene = new Scene(loader.load(), 800, 650);

        // Tema inicial: oscuro hasta que MainController tome el control
        mainScene.getStylesheets().add(AppFX.class.getResource(CSS_BASIC).toExternalForm());
        mainScene.getStylesheets().add(AppFX.class.getResource(CSS_DARK).toExternalForm());

        stage.setTitle("Cliente de Correo IA");
        stage.setScene(mainScene);
        stage.show();
    }

    public static void setMainController(MainController controller) {
        mainController = controller;
    }

    public static MainController getMainController() {
        return mainController;
    }

    public static Scene getMainScene() {
        return mainScene;
    }

    // Llamado desde cualquier parte de la app para cambiar tema
    public static void aplicarTema(boolean light) {
        if (mainController != null) {
            mainController.aplicarTema(light);
        } else if (mainScene != null) {
            // Antes de que MainController exista (pantalla de login)
            mainScene.getStylesheets().clear();
            mainScene.getStylesheets().add(AppFX.class.getResource(CSS_BASIC).toExternalForm());
            String temaCSS = light ? "/styles-light.css" : CSS_DARK;
            mainScene.getStylesheets().add(AppFX.class.getResource(temaCSS).toExternalForm());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}