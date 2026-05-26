package com.teach.javafx;

import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    private static final double LOGIN_WIDTH = 1180;
    private static final double LOGIN_HEIGHT = 760;

    private static Stage mainStage;
    private static boolean canClose = true;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), LOGIN_WIDTH, LOGIN_HEIGHT);
        scene.getStylesheets().add(
                getClass().getResource("css/login-view.css").toExternalForm()
        );

        stage.setTitle("教学管理系统");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.centerOnScreen();
        stage.show();
        stage.setOnCloseRequest(event -> {
            if (canClose) {
                HttpRequestUtil.close();
            } else {
                event.consume();
            }
        });
        mainStage = stage;
    }

    public static void resetStage(String name, Scene scene) {
        mainStage.setTitle(name);
        mainStage.setScene(scene);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        mainStage.setX(bounds.getMinX());
        mainStage.setY(bounds.getMinY());
        mainStage.setWidth(bounds.getWidth());
        mainStage.setHeight(bounds.getHeight());
        mainStage.setMaximized(true);
        mainStage.show();
    }

    public static void loginStage(String name, Scene scene) {
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        mainStage.setWidth(LOGIN_WIDTH);
        mainStage.setHeight(LOGIN_HEIGHT);
        mainStage.setMinWidth(980);
        mainStage.setMinHeight(680);
        mainStage.centerOnScreen();
        mainStage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void setCanClose(boolean canClose) {
        MainApplication.canClose = canClose;
    }
}
