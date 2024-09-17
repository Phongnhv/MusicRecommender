package com.example.mrs;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class MrApplication extends Application {
    double x = 0;
    double y = 0;

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(MrApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        HelloController hello = fxmlLoader.getController();
        hello.setHello(hello);

        scene.setOnMousePressed((MouseEvent event) -> {
            x = event.getSceneX();
            y = event.getSceneY();
        });

        scene.setOnMouseDragged((MouseEvent event) ->{
            stage.setX(event.getScreenX() - x);
            stage.setY(event.getScreenY() - y);
            stage.setOpacity(.8);
        });

        scene.setOnMouseReleased((MouseEvent event) -> stage.setOpacity(1));

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        hello.setStage(stage);
        stage.show();
    }
}