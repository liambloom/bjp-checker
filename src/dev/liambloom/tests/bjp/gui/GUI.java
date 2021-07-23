package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.App;
import javafx.application.Application;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableObjectValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class GUI extends Application {
    public static void main(String[] args) {
        Application.launch(args);
    }

    private static final double SQRT3 = Math.sqrt(3);
    private final SimpleBooleanProperty isProjectOpen = new SimpleBooleanProperty(false);

    @Override
    public void start(Stage stage) {
        // FIXME: When the monitor size changes, the gui scaling doesn't change

        GridPane pane = new GridPane();// hello = new Label("Hello World");
        Scene main = new Scene(pane);

        pane.backgroundProperty().bind(new BackgroundBinding(ColorSchemeManager.getBackgroundProperty()));

        SimpleIntegerProperty sidebarWidth = new SimpleIntegerProperty(200);
        SimpleDoubleProperty chooserDisplayScale = new SimpleDoubleProperty(0.6);

        //ObservableI

        pane.maxWidthProperty().bind(stage.widthProperty());
        pane.minWidthProperty().bind(stage.widthProperty());
        pane.maxHeightProperty().bind(stage.heightProperty());
        pane.minHeightProperty().bind(stage.heightProperty());

        //pane.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        //pane.setGridLinesVisible(true);

        VBox testList = new VBox();
        pane.add(testList, 0, 1);
        Text testTitle = new Text("Tests:");
        testList.prefWidthProperty().bind(sidebarWidth);
        testList.minWidthProperty().bind(sidebarWidth);
        testList.maxWidthProperty().bind(sidebarWidth);
        testTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        testTitle.fillProperty().bind(ColorSchemeManager.getMenuTitleProperty());
        VBox.setMargin(testTitle, new Insets(5));
        //testTitle.minWidthProperty().bind(testList.widthProperty());
        //testTitle.maxWidthProperty().bind(testList.widthProperty());
        testList.getChildren().add(testTitle);
        testList.backgroundProperty().bind(new BackgroundBinding(ColorSchemeManager.getAltBackgroundProperty()));
        testList.minHeightProperty().bind(pane.heightProperty());

        //stage.

        //final double FOLDER_IMG_SCALE = 0.6;
        VBox chooserDisplay = new VBox();
        pane.add(chooserDisplay, 1, 1);
        //GridPane.setRowSpan(chooserDisplay, 2);
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
        //folderImageBack.getTransforms().add(new Scale(0.6, 0.6));
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
        //final double FOLDER_IMG_WIDTH_SCALED = FOLDER_IMG_WIDTH * FOLDER_IMG_SCALE;
        //final double FOLDER_IMG_HEIGHT_SCALED = FOLDER_IMG_HEIGHT * FOLDER_IMG_SCALE;
        folderImagePane.setPrefSize(FOLDER_IMG_WIDTH, FOLDER_IMG_HEIGHT);
        folderImagePane.setMinSize(FOLDER_IMG_WIDTH, FOLDER_IMG_HEIGHT);
        folderImagePane.setMaxSize(FOLDER_IMG_WIDTH, FOLDER_IMG_HEIGHT);
        //folderImagePane.setScaleX(FOLDER_IMG_SCALE);
        //folderImagePane.setScaleY(FOLDER_IMG_SCALE);
        //folderImagePane.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        VBox.setMargin(folderImagePane, new Insets(0, 0, 0, FOLDER_IMG_WIDTH - 450));
        chooserDisplay.setSpacing(10);
        //chooserDisplay.setSpacing(10 * FOLDER_IMG_SCALE);
        //folderImagePane.pad
        folderImageFront.getTransforms().add(new Affine(
                1, -SQRT3 / 3, 280 * SQRT3 / 3,
                0, SQRT3 / 2 , 280 - 140 * SQRT3));

        final int CHOOSER_IMG_GRAY = 30;

        for (SVGPath i : new SVGPath[]{folderImageBack, folderImageFront}) {
            i.fillProperty().bind(ColorSchemeManager.getGrayscaleIconProperty());
            i.strokeProperty().bind(ColorSchemeManager.getBackgroundProperty());
            i.setStrokeWidth(2);
            //AnchorPane.setLeftAnchor(i, 0.0);
            //AnchorPane.setBottomAnchor(i, 0.0);
            folderImagePane.getChildren().add(i);
        }

        chooserDisplay.getChildren().add(folderImagePane);
        Text selectProjectText = new Text("Open Project");
        selectProjectText.fillProperty().bind(ColorSchemeManager.getGrayscaleIconProperty());
        selectProjectText.setFont(Font.font("Arial", FontWeight.BOLD, 70));
        chooserDisplay.getChildren().add(selectProjectText);
        //GridPane.setHalignment(chooserDisplay, HPos.CENTER);
        //GridPane.setValignment(chooserDisplay, VPos.CENTER);
        chooserDisplay.setAlignment(Pos.CENTER);
        chooserDisplay.setOnMouseClicked(e -> openProject(stage));

        //chooserDisplay.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        //ColumnConstraints mainColumn = new ColumnConstraints();
        chooserDisplay.prefWidthProperty().bind(pane.widthProperty().subtract(sidebarWidth));
        chooserDisplay.prefHeightProperty().bind(pane.heightProperty());

        chooserDisplay.scaleXProperty().bind(chooserDisplayScale);
        chooserDisplay.scaleYProperty().bind(chooserDisplayScale);
        chooserDisplay.maxWidthProperty().bind(pane.widthProperty().subtract(sidebarWidth));
        chooserDisplay.minWidthProperty().bind(chooserDisplayScale.multiply(FOLDER_IMG_WIDTH * 2 - 450)); // TODO: This could be a constant
        //chooserDisplay.minWidthProperty().bind(chooserDisplayScale.add(selectProjectText.hei)); // .multiply(FOLDER_IMG_HEIGHT)

        MenuBar menuBar = new MenuBar();
        //Platform.runLater(() -> menuBar.setUseSystemMenuBar(true));
        menuBar.setUseSystemMenuBar(true); // This only works on "supported platforms," which does not include windows
        System.out.println(menuBar.isUseSystemMenuBar());
        Menu fileMenu = new Menu("File");
        menuBar.getMenus().add(fileMenu);
        MenuItem openProject = new MenuItem("Open Project");
        openProject.setOnAction(e -> openProject(stage));
        MenuItem selectFile = new MenuItem("Open File");
        Menu openRecent = new Menu("Open Recent");
        openRecent.setDisable(App.prefs().get("recent", "").equals(""));
        MenuItem addFile = new MenuItem("Add File to Project");
        addFile.disableProperty().bind(isProjectOpen.not());
        Menu settingsMenuItem = new Menu("Settings");
        CheckMenuItem darkMode = new CheckMenuItem("Dark Mode");
        settingsMenuItem.getItems().add(darkMode);
        darkMode.setSelected(ColorSchemeManager.getColorScheme().equals(ColorSchemeManager.getDarkColorScheme()));
        ColorSchemeManager.colorSchemeProperty().bind(new ObjectBinding<>() {
            { bind(darkMode.selectedProperty()); }

            @Override
            protected ColorScheme computeValue() {
                return darkMode.isSelected() ? ColorSchemeManager.getDarkColorScheme() : ColorSchemeManager.getLightColorScheme();
            }
        });
        //colorScheme.getItems().addAll(new SeparatorMenuItem())
        fileMenu.getItems().addAll(openProject, selectFile, openRecent, addFile, new SeparatorMenuItem(), settingsMenuItem);
        pane.add(menuBar, 0, 0);
        GridPane.setColumnSpan(menuBar, pane.getColumnCount());

//        Pane settingPane = new Pane();
//        Scene settings = new Scene(settingPane);
//
//        VBox settingsContent = new VBox();
//        settingPane.getChildren().add(settingsContent);
//        settingsContent.getChildren().addAll(new Text("Foo"), new Text("FooBar"));
//        settingsContent.setAlignment(Pos.CENTER);

        //settingsMenuItem.setOnAction(e -> {
//            double width = stage.getWidth();
//            double height = stage.getHeight();
//            stage.setScene(settings);
//            stage.setWidth(width);
//            stage.setHeight(height);
        //});


        //pane.setGridLinesVisible(true);

        //stage.minHeightProperty().bind(pane.hei());

        //mainColumn.prefWidthProperty().bind(stage.widthProperty().subtract(pane.getColumnConstraints().get))
        //DirectoryChooser chooser = new DirectoryChooser();
        //pane.add(chooser, 0, 1);
        stage.minWidthProperty().bind(chooserDisplayScale.multiply(FOLDER_IMG_WIDTH * 2 - 450).add(sidebarWidth));
        //stage.minHeightProperty().bind(chooserDisplayScale.multiply(chooserDisplay.heightProperty()));

        stage.setTitle("BJP Checker");
        stage.setScene(main);
        stage.show();

        //System.out.printf("min: %f, max: %f, pref: %f, actual: %f", folderImagePane.getMinWidth(), folderImagePane.getMaxWidth(), folderImagePane.getPrefWidth(), folderImagePane.getWidth());
    }

    private void openProject(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project");
        chooser.showDialog(stage);
        // TODO
    }

    private static ObjectBinding<Background> getBoundBackgroundProperty(ObservableObjectValue<Paint> c) {
        return new ObjectBinding<>() {
            { bind(c); }

            @Override
            protected Background computeValue() {
                return new Background(new BackgroundFill(c.get(), CornerRadii.EMPTY, Insets.EMPTY));
            }
        };
    }
}
