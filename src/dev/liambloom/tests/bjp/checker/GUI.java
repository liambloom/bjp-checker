package dev.liambloom.tests.bjp.checker;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

public class GUI extends Application {
    public static void main(String[] args) {
        App.cleanArgs(args);
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        GridPane pane = new GridPane();// hello = new Label("Hello World");
        Scene main = new Scene(pane);

        SimpleIntegerProperty sidebarWidth = new SimpleIntegerProperty(200);

        //ObservableI

        /*pane.maxWidthProperty().bind(stage.widthProperty());
        pane.minWidthProperty().bind(stage.widthProperty());
        pane.maxHeightProperty().bind(stage.heightProperty());
        pane.minHeightProperty().bind(stage.heightProperty());*/

        pane.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        pane.setGridLinesVisible(true);

        VBox testList = new VBox();
        pane.add(testList, 0, 0);
        Text testTitle = new Text("Tests:");
        testList.prefWidthProperty().bind(sidebarWidth);
        //testTitle.minWidthProperty().bind(testList.widthProperty());
        //testTitle.maxWidthProperty().bind(testList.widthProperty());
        testList.getChildren().add(testTitle);

        VBox chooserDisplay = new VBox();
        pane.add(chooserDisplay, 1, 0);
        GridPane.setRowSpan(chooserDisplay, 2);
        SVGPath folderImage = new SVGPath();
        folderImage.setContent(""); // TODO
        // There's overlapping sections, how will that work. Maybe a border that's the background color?
        folderImage.fillProperty().bind(ColorScheme.getGrayProperty());
        chooserDisplay.getChildren().add(folderImage);
        chooserDisplay.getChildren().add(new Text("Select Project"));
        //GridPane.setHalignment(chooserDisplay, HPos.CENTER);
        //GridPane.setValignment(chooserDisplay, VPos.CENTER);
        chooserDisplay.setAlignment(Pos.CENTER);
        chooserDisplay.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        //ColumnConstraints mainColumn = new ColumnConstraints();
        chooserDisplay.prefWidthProperty().bind(pane.widthProperty().subtract(sidebarWidth));
        chooserDisplay.prefHeightProperty().bind(pane.heightProperty());
        //mainColumn.prefWidthProperty().bind(stage.widthProperty().subtract(pane.getColumnConstraints().get))
        //DirectoryChooser chooser = new DirectoryChooser();
        //pane.add(chooser, 0, 1);

        stage.setTitle("BJP Checker");
        stage.setScene(main);
        stage.show();
    }
}
