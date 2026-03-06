package com.emailAI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(AppFX.class.getResource("/ui/login-view.fxml"));
        Scene scene = new Scene(loader.load(), 470, 350);

        // Aplicar tema oscuro por defecto
        scene.getStylesheets().add(
                AppFX.class.getResource("/styles-dark.css").toExternalForm()
        );

        stage.setTitle("Cliente de Correo IA - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

