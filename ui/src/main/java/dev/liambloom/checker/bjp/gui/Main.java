package dev.liambloom.checker.bjp.gui;

import dev.liambloom.checker.bjp.api.App;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main extends Application {
    public static void main(String[] args) {
        Application.launch(args);
    }

//    private static final double SQRT3 = Math.sqrt(3);
//    private static final int DOTS_PANE_SIZE = 16;
//    private final SimpleBooleanProperty isProjectOpen = new SimpleBooleanProperty(false);

    private ColorScheme colorScheme;
    private ArrayList<Scene> scenes = new ArrayList<>(1);

    public Main() {
        setColorScheme(ColorScheme.valueOf(App.prefs().get("colorScheme", ColorScheme.LIGHT.name())));
    }

    @Override
    public void start(Stage stage) {
        // FIXME: When the monitor size changes, the gui scaling doesn't change
        GridPane content;
        try {
            content = FXMLLoader.load(Main.class.getClassLoader().getResource("views/Main.fxml"));
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Scene scene = new Scene(content);
        scene.getStylesheets().addAll(Stream.of("Main")
            .map(s -> "css/" + s + ".css")
            .map(Main.class.getClassLoader()::getResource)
            .map(URL::toExternalForm)
            .collect(Collectors.toList()));
        scenes.add(scene);
        addColorSchemeStyles(scene);

        content.maxWidthProperty().bind(stage.widthProperty());
        content.minWidthProperty().bind(stage.widthProperty());
        content.maxHeightProperty().bind(stage.heightProperty());
        content.minHeightProperty().bind(stage.heightProperty());

        stage.setTitle("Checker for BJP");
        stage.setScene(scene);
        stage.show();
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
        App.prefs().put("colorScheme", colorScheme.name());
        scenes.forEach(this::addColorSchemeStyles);
    }

    private void addColorSchemeStyles(Scene scene) {
        scene.getStylesheets().add(Main.class.getClassLoader().getResource("css/" + getColorScheme().name().toLowerCase(Locale.ENGLISH) + "-theme.css").toExternalForm());
    }

//    public void start2(Stage stage) {
//        SimpleDoubleProperty chooserDisplayScale = new SimpleDoubleProperty(0.6);
//
//        Button listMenuButton = new Button();
//        listMenuButton.setPadding(Insets.EMPTY);
//        SVGPath listMenuButtonIcon = new SVGPath();
//        listMenuButtonIcon.setContent("M 7 3 h 2 v 4 h 4 v 2 h -4 v 4 h -2 v -4 h -4 v -2 h 4 z");
//        listMenuButtonIcon.fillProperty().bind(ColorSchemeManager.getTitleProperty());
//        listMenuButton.setGraphic(listMenuButtonIcon);
//        listMenuButton.setMinWidth(DOTS_PANE_SIZE);
//        listMenuButton.setMaxWidth(DOTS_PANE_SIZE);
//        listMenuButton.setMinHeight(DOTS_PANE_SIZE);
//        listMenuButton.setMaxHeight(DOTS_PANE_SIZE);
//        listMenuButton.setOnAction(GUI::addTests);
//
//        listMenuButton.backgroundProperty().bind(new ObjectBinding<>() {
//            { bind(ColorSchemeManager.getAltBackgroundHoverProperty(), listMenuButton.hoverProperty()); }
//
//            @Override
//            protected Background computeValue() {
//                return listMenuButton.isHover()
//                        ? new Background(new BackgroundFill(ColorSchemeManager.getAltBackgroundHoverProperty().get(), new CornerRadii(2), Insets.EMPTY))
//                        : Background.EMPTY;
//            }
//        });
//        ContextMenu testListMenu = new ContextMenu();
//        MenuItem addTestMenuItem = new MenuItem("Add Test");
//        addTestMenuItem.setOnAction(GUI::addTests);
//
//        testListMenu.getItems().add(addTestMenuItem);
//        testList.setOnContextMenuRequested(e -> {
//            Bounds bounds = testList.localToScreen(testList.getBoundsInLocal());
//            testListMenu.show(testList, e.getX() + bounds.getMinX(), e.getY() + bounds.getMinY());
//            testList.setOnMouseClicked(e2 -> {
//                testListMenu.hide();
//                testList.setOnMouseClicked(null);
//            });
//            testListMenu.setOnAutoHide(e2 -> testList.setOnMouseClicked(null));
//        });
//
//
//        VBox.setMargin(testListHeader, new Insets(TEST_LIST_MARGIN));
//        //testTitle.minWidthProperty().bind(testList.widthProperty());
//        //testTitle.maxWidthProperty().bind(testList.widthProperty());
//        AnchorPane.setRightAnchor(listMenuButton, 0.0);
//        testListHeader.getChildren().addAll(testTitle, listMenuButton);
//        testList.getChildren().add(testListHeader);
//        testList.backgroundProperty().bind(new BackgroundBinding(ColorSchemeManager.getAltBackgroundProperty()));
//        testList.minHeightProperty().bind(pane.heightProperty());
//
//        // TODO: add a way to select a test
//        // TODO: show test validation results
//        ToggleGroup selectedTest = new ToggleGroup();
//        List<Label> books = Book.getAllTests()
//                .map(GUI::testNode)
//                .collect(Collectors.toList());
//        ((RadioButton) books.get(0).getLabelFor()).setSelected(true);
//        testList.getChildren().addAll(books);
//
//        //stage.
//
//        //final double FOLDER_IMG_SCALE = 0.6;
//        StackPane chooserDisplayWrapper = new StackPane();
//        VBox chooserDisplay = new VBox();
//        chooserDisplayWrapper.getChildren().add(chooserDisplay);
//        pane.add(chooserDisplayWrapper, 1, 1);
//        //GridPane.setRowSpan(chooserDisplay, 2);
//        Pane folderImagePane = new Pane();
//        SVGPath folderImageBack = new SVGPath();
//        folderImageBack.setContent(String.format("""
//            M 0 280
//            V 20
//            a 8 8 0 0 0 %1$f -4
//            l %1$f -12
//            a 8 8 0 0 1 %1$f -4
//            h %2$f
//            a 8 8 0 0 1 %1$f 4
//            l %1$f 12
//            a 8 8 0 0 0 %1$f 4
//            h %3$f
//            a 8 8 0 0 1 %1$f 4
//            l %1$f 12
//            a 8 8 0 0 0 %1$f 4
//            V 280
//            z
//            """, 4 * SQRT3, 150 - 24 * SQRT3, 300 - 12 * SQRT3));
//        //folderImageBack.getTransforms().add(new Scale(0.6, 0.6));
//        //folderImage.getTransforms().
//        // There's overlapping sections, how will that work. Maybe a border that's the background color?
//        SVGPath folderImageFront = new SVGPath();
//        folderImageFront.setContent(String.format("""
//                M 0 280
//                V 65
//                a 8 8 0 0 0 %1$f -4
//                l %1$f -12
//                a 8 8 0 0 1 %1$f -4
//                h %2$f
//                a 8 8 0 0 0 %1$f -4
//                l %1$f -12
//                a 8 8 0 0 1 %1$f -4
//                h %3$f
//                a 8 8 0 0 1 %1$f 4
//                l %1$f 12
//                a 8 8 0 0 0 %1$f 4
//                V 280
//                z
//                """, 4 * SQRT3, 150 - 24 * SQRT3, 300 - 12 * SQRT3));
//        final double FOLDER_IMG_WIDTH = 450 - 15 * SQRT3 + 280 * SQRT3 / 3;
//        final double FOLDER_IMG_HEIGHT = 280 * SQRT3 / 2 + 280 - 140 * SQRT3;
//        //final double FOLDER_IMG_WIDTH_SCALED = FOLDER_IMG_WIDTH * FOLDER_IMG_SCALE;
//        //final double FOLDER_IMG_HEIGHT_SCALED = FOLDER_IMG_HEIGHT * FOLDER_IMG_SCALE;
//        folderImagePane.setPrefSize(FOLDER_IMG_WIDTH, FOLDER_IMG_HEIGHT);
//        folderImagePane.setMinSize(FOLDER_IMG_WIDTH, FOLDER_IMG_HEIGHT);
//        folderImagePane.setMaxSize(FOLDER_IMG_WIDTH, FOLDER_IMG_HEIGHT);
//        //folderImagePane.setScaleX(FOLDER_IMG_SCALE);
//        //folderImagePane.setScaleY(FOLDER_IMG_SCALE);
//        //folderImagePane.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
//        VBox.setMargin(folderImagePane, new Insets(0, 0, 0, FOLDER_IMG_WIDTH - 450));
//        chooserDisplay.setSpacing(10);
//        //chooserDisplay.setSpacing(10 * FOLDER_IMG_SCALE);
//        //folderImagePane.pad
//        folderImageFront.getTransforms().add(new Affine(
//                1, -SQRT3 / 3, 280 * SQRT3 / 3,
//                0, SQRT3 / 2 , 280 - 140 * SQRT3));
//
//        for (SVGPath i : new SVGPath[]{folderImageBack, folderImageFront}) {
//            i.fillProperty().bind(ColorSchemeManager.getGrayscaleIconProperty());
//            i.strokeProperty().bind(ColorSchemeManager.getBackgroundProperty());
//            i.setStrokeWidth(2);
//            //AnchorPane.setLeftAnchor(i, 0.0);
//            //AnchorPane.setBottomAnchor(i, 0.0);
//            folderImagePane.getChildren().add(i);
//        }
//
//        chooserDisplay.getChildren().add(folderImagePane);
//        Text selectProjectText = new Text("Open Project");
//        selectProjectText.fillProperty().bind(ColorSchemeManager.getGrayscaleIconProperty());
//        selectProjectText.setFont(Font.font("Arial", FontWeight.BOLD, 70));
//        chooserDisplay.getChildren().add(selectProjectText);
//        //GridPane.setHalignment(chooserDisplay, HPos.CENTER);
//        //GridPane.setValignment(chooserDisplay, VPos.CENTER);
//        chooserDisplay.setAlignment(Pos.CENTER);
//        chooserDisplayWrapper.setOnMouseClicked(e -> openProject(stage));
//
//        //chooserDisplay.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
//        //ColumnConstraints mainColumn = new ColumnConstraints();
//        chooserDisplay.prefWidthProperty().bind(pane.widthProperty().subtract(sidebarWidth));
//        chooserDisplay.prefHeightProperty().bind(pane.heightProperty());
//
//        chooserDisplay.scaleXProperty().bind(chooserDisplayScale);
//        chooserDisplay.scaleYProperty().bind(chooserDisplayScale);
//        chooserDisplay.maxWidthProperty().bind(pane.widthProperty().subtract(sidebarWidth));
//        chooserDisplay.minWidthProperty().bind(chooserDisplayScale.multiply(FOLDER_IMG_WIDTH * 2 - 450)); // TODO: This could be a constant
//        //chooserDisplay.minWidthProperty().bind(chooserDisplayScale.add(selectProjectText.hei)); // .multiply(FOLDER_IMG_HEIGHT)
//
//        MenuBar menuBar = new MenuBar();
//        //Platform.runLater(() -> menuBar.setUseSystemMenuBar(true));
//        menuBar.setUseSystemMenuBar(true); // This only works on "supported platforms," which does not include windows
//        Menu fileMenu = new Menu("File");
//        menuBar.getMenus().add(fileMenu);
//        MenuItem openProject = new MenuItem("Open Project");
//        openProject.setOnAction(e -> openProject(stage));
//        MenuItem selectFile = new MenuItem("Open File");
//        selectFile.setOnAction(e -> notYetSupported());
//        Menu openRecent = new Menu("Open Recent");
//        openRecent.setDisable(App.prefs().get("recent", "").equals(""));
//        MenuItem addFile = new MenuItem("Add File to Project");
//        addFile.disableProperty().bind(isProjectOpen.not());
//        Menu settingsMenuItem = new Menu("Settings");
//        CheckMenuItem darkMode = new CheckMenuItem("Dark Mode");
//        settingsMenuItem.getItems().add(darkMode);
//        darkMode.setSelected(ColorSchemeManager.getColorScheme().equals(ColorSchemeManager.getDarkColorScheme()));
//        ColorSchemeManager.colorSchemeProperty().bind(new ObjectBinding<>() {
//            { bind(darkMode.selectedProperty()); }
//
//            @Override
//            protected ColorScheme computeValue() {
//                return darkMode.isSelected() ? ColorSchemeManager.getDarkColorScheme() : ColorSchemeManager.getLightColorScheme();
//            }
//        });
//        //colorScheme.getItems().addAll(new SeparatorMenuItem())
//        fileMenu.getItems().addAll(openProject, selectFile, openRecent, addFile, new SeparatorMenuItem(), settingsMenuItem);
//        pane.add(menuBar, 0, 0);
//        GridPane.setColumnSpan(menuBar, pane.getColumnCount());
//
////        Pane settingPane = new Pane();
////        Scene settings = new Scene(settingPane);
////
////        VBox settingsContent = new VBox();
////        settingPane.getChildren().add(settingsContent);
////        settingsContent.getChildren().addAll(new Text("Foo"), new Text("FooBar"));
////        settingsContent.setAlignment(Pos.CENTER);
//
//        //settingsMenuItem.setOnAction(e -> {
////            double width = stage.getWidth();
////            double height = stage.getHeight();
////            stage.setScene(settings);
////            stage.setWidth(width);
////            stage.setHeight(height);
//        //});
//
//
//        //pane.setGridLinesVisible(true);
//
//        //stage.minHeightProperty().bind(pane.hei());
//
//        //mainColumn.prefWidthProperty().bind(stage.widthProperty().subtract(pane.getColumnConstraints().get))
//        //DirectoryChooser chooser = new DirectoryChooser();
//        //pane.add(chooser, 0, 1);
//        stage.minWidthProperty().bind(chooserDisplayScale.multiply(FOLDER_IMG_WIDTH * 2 - 450).add(sidebarWidth));
//        //stage.minHeightProperty().bind(chooserDisplayScale.multiply(chooserDisplay.heightProperty()));
//
//        stage.setTitle("BJP Checker");
//        stage.setScene(main);
//        stage.show();
//
//        //System.out.printf("min: %f, max: %f, pref: %f, actual: %f", folderImagePane.getMinWidth(), folderImagePane.getMaxWidth(), folderImagePane.getPrefWidth(), folderImagePane.getWidth());
//    }
//
//    private void openProject(Stage stage) {
//        DirectoryChooser chooser = new DirectoryChooser();
//        chooser.setTitle("Open Project");
//        File f = chooser.showDialog(stage);
//        if (f == null)
//            return;
//        Path path = f.toPath();
//        notYetSupported();
//    }
//

//
//    private static void notYetSupported() {
//        Alert alert = new Alert(Alert.AlertType.ERROR);
//        alert.setHeaderText(null);
//        alert.setContentText("That functionality is not supported in checker " + App.VERSION);
//        alert.showAndWait();
//    }
}
