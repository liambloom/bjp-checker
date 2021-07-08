package dev.liambloom.tests.bjp.checker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class GUI extends Application {
    public static void main(String[] args) {
        App.cleanArgs(args);
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        GridPane pane = new GridPane();// hello = new Label("Hello World");
        Scene main = new Scene(pane);

        VBox testList = new VBox();
        pane.add(testList, 0, 0);
        testList.getChildren().add(new Label("Tests:"));

        VBox chooserDisplay = new VBox();
        pane.add(chooserDisplay, 1, 0);
        GridPane.setRowSpan(chooserDisplay, 2);
        chooserDisplay.getChildren().add(new Label("FOLDER IMAGE HERE"));
        chooserDisplay.getChildren().add(new Label("Select Project"));
        //DirectoryChooser chooser = new DirectoryChooser();
        //pane.add(chooser, 0, 1);

        stage.setScene(main);
        stage.show();
    }
}
