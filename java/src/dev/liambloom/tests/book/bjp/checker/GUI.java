package dev.liambloom.tests.book.bjp.checker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class GUI extends Application {
    @Override
    public void start(Stage stage) {
        Label hello = new Label("Hello World");
        Scene main = new Scene(hello);

        stage.setScene(main);
        stage.show();
    }
}
