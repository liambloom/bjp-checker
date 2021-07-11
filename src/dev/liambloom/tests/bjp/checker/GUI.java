package dev.liambloom.tests.bjp.checker;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

public class GUI extends Application {
    public static void main(String[] args) {
        App.cleanArgs(args);
        Application.launch(args);
    }

    private static final double SQRT3 = Math.sqrt(3);

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

        final double FOLDER_IMG_SCALE = 0.6;
        VBox chooserDisplay = new VBox();
        pane.add(chooserDisplay, 1, 0);
        GridPane.setRowSpan(chooserDisplay, 2);
        Pane folderImagePane = new Pane();
        SVGPath folderImageBack = new SVGPath();
        folderImageBack.setContent(String.format("""
            M 0 280
            V 20
            a 8 8 0 0 0 %1$f -4
            l %1$f -12
            a 8 8 0 0 1 %1$f -4
            h %2$f
            a 8 8 0 0 1 %1$f 4
            l %1$f 12
            a 8 8 0 0 0 %1$f 4
            h %3$f
            a 8 8 0 0 1 %1$f 4
            l %1$f 12
            a 8 8 0 0 0 %1$f 4
            V 280
            z
            """, 4 * SQRT3, 150 - 24 * SQRT3, 300 - 12 * SQRT3));
        folderImageBack.getTransforms().add(new Scale(FOLDER_IMG_SCALE, FOLDER_IMG_SCALE));
        //folderImage.getTransforms().
        // There's overlapping sections, how will that work. Maybe a border that's the background color?
        SVGPath folderImageFront = new SVGPath();
        folderImageFront.setContent(String.format("""
                M 0 280
                V 65
                a 8 8 0 0 0 %1$f -4
                l %1$f -12
                a 8 8 0 0 1 %1$f -4
                h %2$f
                a 8 8 0 0 0 %1$f -4
                l %1$f -12
                a 8 8 0 0 1 %1$f -4
                h %3$f
                a 8 8 0 0 1 %1$f 4
                l %1$f 12
                a 8 8 0 0 0 %1$f 4
                V 280
                z
                """, 4 * SQRT3, 150 - 24 * SQRT3, 300 - 12 * SQRT3));
        final double FOLDER_IMG_WIDTH = 450 - 15 * SQRT3 + 280 * SQRT3 / 3;
        final double FOLDER_IMG_HEIGHT = 280 * SQRT3 / 2 + 280 - 140 * SQRT3;
        final double FOLDER_IMG_WIDTH_SCALED = FOLDER_IMG_WIDTH * FOLDER_IMG_SCALE;
        final double FOLDER_IMG_HEIGHT_SCALED = FOLDER_IMG_HEIGHT * FOLDER_IMG_SCALE;
        folderImagePane.setPrefSize(FOLDER_IMG_WIDTH_SCALED, FOLDER_IMG_HEIGHT_SCALED);
        folderImagePane.setMinSize(FOLDER_IMG_WIDTH_SCALED, FOLDER_IMG_HEIGHT_SCALED);
        folderImagePane.setMaxSize(FOLDER_IMG_WIDTH_SCALED, FOLDER_IMG_HEIGHT_SCALED);
        //folderImagePane.setScaleX(FOLDER_IMG_SCALE);
        //folderImagePane.setScaleY(FOLDER_IMG_SCALE);
        //folderImagePane.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        VBox.setMargin(folderImagePane, new Insets(0, 0, 0, (FOLDER_IMG_WIDTH - 450) * FOLDER_IMG_SCALE));
        chooserDisplay.setSpacing(10 * FOLDER_IMG_SCALE);
        //chooserDisplay.setSpacing(10 * FOLDER_IMG_SCALE);
        //folderImagePane.pad
        folderImageFront.getTransforms().add(new Affine(
                FOLDER_IMG_SCALE, -SQRT3 / 3 * FOLDER_IMG_SCALE, 280 * SQRT3 / 3 * FOLDER_IMG_SCALE,
                0, SQRT3 / 2 * FOLDER_IMG_SCALE ,(280 - 140 * SQRT3) * FOLDER_IMG_SCALE));

        for (SVGPath i : new SVGPath[]{folderImageBack, folderImageFront}) {
            i.fillProperty().bind(ColorScheme.getGrayProperty());
            i.strokeProperty().bind(ColorScheme.getBackgroundProperty());
            //AnchorPane.setLeftAnchor(i, 0.0);
            //AnchorPane.setBottomAnchor(i, 0.0);
            folderImagePane.getChildren().add(i);
        }

        chooserDisplay.getChildren().add(folderImagePane);
        Text selectProjectText = new Text("Select Project");
        selectProjectText.fillProperty().bind(ColorScheme.getGrayProperty());
        selectProjectText.setFont(Font.font("Arial", FontWeight.BOLD, 70 * FOLDER_IMG_SCALE));
        chooserDisplay.getChildren().add(selectProjectText);
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

        //System.out.printf("min: %f, max: %f, pref: %f, actual: %f", folderImagePane.getMinWidth(), folderImagePane.getMaxWidth(), folderImagePane.getPrefWidth(), folderImagePane.getWidth());
    }
}
