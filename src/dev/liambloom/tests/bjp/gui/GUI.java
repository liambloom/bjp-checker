package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.*;
import javafx.application.Application;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class GUI extends Application {
    public static void main(String[] args) {
        Application.launch(args);
    }

    private static final double SQRT3 = Math.sqrt(3);
    private static final int DOTS_PANE_SIZE = 16;
    private final SimpleBooleanProperty isProjectOpen = new SimpleBooleanProperty(false);
//    public void start2(Stage stage) {
//        Pane pane = new Pane();
//        Text text = new Text(10, 20, "Foo");
//        Bounds bounds = text.getBoundsInLocal();
//        Rectangle box = new Rectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
//        box.setFill(Color.TRANSPARENT);
//        box.setStroke(Color.RED);
//        pane.getChildren().add(text);
//        pane.getChildren().add(box);
//        stage.setScene(new Scene(pane));
//        stage.show();
//    }

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
        final double TEST_LIST_MARGIN = 5.0;
        pane.add(testList, 0, 1);
        AnchorPane testListHeader = new AnchorPane();
        Label testTitle = new Label("Tests:");
        testList.minWidthProperty().bind(sidebarWidth);
        testList.maxWidthProperty().bind(sidebarWidth);
        testList.prefWidthProperty().bind(sidebarWidth);
        testTitle.maxWidthProperty().bind(sidebarWidth.subtract(TEST_LIST_MARGIN * 3 + DOTS_PANE_SIZE));
        testTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        testTitle.textFillProperty().bind(ColorSchemeManager.getTitleProperty());

        Button listMenuButton = new Button();
        listMenuButton.setPadding(Insets.EMPTY);
        SVGPath listMenuButtonIcon = new SVGPath();
        listMenuButtonIcon.setContent("M 7 3 h 2 v 4 h 4 v 2 h -4 v 4 h -2 v -4 h -4 v -2 h 4 z");
        listMenuButtonIcon.fillProperty().bind(ColorSchemeManager.getTitleProperty());
        listMenuButton.setGraphic(listMenuButtonIcon);
        listMenuButton.setMinWidth(DOTS_PANE_SIZE);
        listMenuButton.setMaxWidth(DOTS_PANE_SIZE);
        listMenuButton.setMinHeight(DOTS_PANE_SIZE);
        listMenuButton.setMaxHeight(DOTS_PANE_SIZE);
        listMenuButton.setOnAction(GUI::addTests);

        listMenuButton.backgroundProperty().bind(new ObjectBinding<>() {
            { bind(ColorSchemeManager.getAltBackgroundHoverProperty(), listMenuButton.hoverProperty()); }

            @Override
            protected Background computeValue() {
                return listMenuButton.isHover()
                        ? new Background(new BackgroundFill(ColorSchemeManager.getAltBackgroundHoverProperty().get(), new CornerRadii(2), Insets.EMPTY))
                        : Background.EMPTY;
            }
        });
        ContextMenu testListMenu = new ContextMenu();
        MenuItem addTestMenuItem = new MenuItem("Add Test");
        addTestMenuItem.setOnAction(GUI::addTests);

        testListMenu.getItems().add(addTestMenuItem);
        testList.setOnContextMenuRequested(e -> {
            Bounds bounds = testList.localToScreen(testList.getBoundsInLocal());
            testListMenu.show(testList, e.getX() + bounds.getMinX(), e.getY() + bounds.getMinY());
            testList.setOnMouseClicked(e2 -> {
                testListMenu.hide();
                testList.setOnMouseClicked(null);
            });
            testListMenu.setOnAutoHide(e2 -> testList.setOnMouseClicked(null));
        });


        VBox.setMargin(testListHeader, new Insets(TEST_LIST_MARGIN));
        //testTitle.minWidthProperty().bind(testList.widthProperty());
        //testTitle.maxWidthProperty().bind(testList.widthProperty());
        AnchorPane.setRightAnchor(listMenuButton, 0.0);
        testListHeader.getChildren().addAll(testTitle, listMenuButton);
        testList.getChildren().add(testListHeader);
        testList.backgroundProperty().bind(new BackgroundBinding(ColorSchemeManager.getAltBackgroundProperty()));
        testList.minHeightProperty().bind(pane.heightProperty());

        // TODO: add a way to select a test
        // TODO: show test validation results
        testList.getChildren().addAll(
                Book.getAllTests()
                    .map(book -> {
                        // I think a grid would be better for this
                        AnchorPane bookPane = new AnchorPane();
                        bookPane.minWidthProperty().bind(testList.widthProperty());
                        bookPane.maxWidthProperty().bind(testList.widthProperty());
                        Label bookName = new Label(book.getName());
                        bookName.textFillProperty().bind(ColorSchemeManager.getTitleProperty());
                        bookName.maxWidthProperty().bind(sidebarWidth.subtract(TEST_LIST_MARGIN * 3).subtract(DOTS_PANE_SIZE));
                        AnchorPane.setTopAnchor(bookName, TEST_LIST_MARGIN);
                        AnchorPane.setLeftAnchor(bookName, TEST_LIST_MARGIN);
                        bookPane.getChildren().add(bookName);
                        MenuItem changePath = new MenuItem("Change Path");
                        double pathHeight;
                        if (book instanceof PathBook pathBook) {
                            Label bookPath = new Label(pathBook.getPath().getParent() + File.separator);
                            Label bookFileName = new Label(pathBook.getPath().getFileName().toString());
                            bookPath.maxWidthProperty().bind(testList.widthProperty().subtract(bookFileName.widthProperty()).subtract(TEST_LIST_MARGIN * 2));
                            bookPath.textFillProperty().bind(ColorSchemeManager.getSubTitleProperty());
                            bookFileName.setFont(bookPath.getFont());
                            bookFileName.textFillProperty().bind(ColorSchemeManager.getSubTitleProperty());
                            AnchorPane.setBottomAnchor(bookPath, TEST_LIST_MARGIN);
                            AnchorPane.setLeftAnchor(bookPath, TEST_LIST_MARGIN);
                            AnchorPane.setBottomAnchor(bookFileName, TEST_LIST_MARGIN);
                            AnchorPane.setRightAnchor(bookFileName, TEST_LIST_MARGIN); // TODO: better ellipsis positioning
                            //bookPath.widthProperty().addListener((a1, a2, a3) -> AnchorPane.setLeftAnchor(bookFileName, bookPath.getWidth() + TEST_LIST_MARGIN));
                            bookPane.getChildren().addAll(bookPath, bookFileName);

                            Text dummyPathText = new Text();
                            dummyPathText.setFont(bookPath.getFont());
                            pathHeight = dummyPathText.getBoundsInLocal().getHeight();

                            changePath.setOnAction(e -> {
                                FileChooser chooser = new FileChooser();
                                chooser.setTitle("Open Test");
                                if (Files.exists(pathBook.getPath()))
                                    chooser.setInitialDirectory(pathBook.getPath().getParent().toFile());
                                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                                Optional.ofNullable(chooser.showOpenDialog(stage))
                                        .map(File::toPath)
                                        .ifPresent((ConsumerThrowsIOException<Path>) p -> {
                                            pathBook.setPath(p);
                                            bookPath.setText(p.getParent().toString() + File.separator);
                                            bookFileName.setText(p.getFileName().toString());
                                        });
                            });
                        }
                        else {
                            pathHeight = 0;
                            changePath.setDisable(true);
                        }
                        Text dummyTitleText = new Text();
                        dummyTitleText.setFont(bookName.getFont());
                        double height = TEST_LIST_MARGIN * (pathHeight == 0 ? 2 : 3) + dummyTitleText.getBoundsInLocal().getHeight() + pathHeight;
                        bookPane.setMinHeight(height);
                        bookPane.setMaxHeight(height);
                        Pane threeDots = new Pane();
                        /*threeDots.setMinSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);
                        threeDots.setMaxSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);*/
                        final double DOT_RADIUS = 1.2;
                        Circle topDot = new Circle(DOTS_PANE_SIZE / 2.0, 4, DOT_RADIUS);
                        topDot.fillProperty().bind(ColorSchemeManager.getTitleProperty());
                        Circle middleDot = new Circle(DOTS_PANE_SIZE / 2.0, DOTS_PANE_SIZE / 2.0, DOT_RADIUS);
                        middleDot.fillProperty().bind(ColorSchemeManager.getTitleProperty());
                        Circle bottomDot = new Circle(DOTS_PANE_SIZE / 2.0, 12, DOT_RADIUS);
                        bottomDot.fillProperty().bind(ColorSchemeManager.getTitleProperty());
                        threeDots.getChildren().addAll(topDot, middleDot, bottomDot);
                        Button menuButton = new Button();
                        menuButton.setGraphic(threeDots);
                        menuButton.setPadding(Insets.EMPTY);
                        //menuButton.setBackground(Background.EMPTY);
                        //menuButton.getItems().add(new MenuItem("Delete"));
                        menuButton.setMinSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);
                        menuButton.setMaxSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);
                        menuButton.backgroundProperty().bind(new ObjectBinding<>() {
                            { bind(ColorSchemeManager.getAltBackgroundHoverProperty(), menuButton.hoverProperty()); }

                            @Override
                            protected Background computeValue() {
                                return menuButton.isHover()
                                        ? new Background(new BackgroundFill(ColorSchemeManager.getAltBackgroundHoverProperty().get(), new CornerRadii(2), Insets.EMPTY))
                                        : Background.EMPTY;
                            }
                        });
                        ContextMenu menu = new ContextMenu();
                        MenuItem rename = new MenuItem("Rename");
                        MenuItem delete = new MenuItem("Delete");
                        if (book instanceof ModifiableBook modifiableBook) {
                            rename.setOnAction(e -> {
                                TextInputDialog dialog = new TextInputDialog(book.getName());
                                dialog.setHeaderText(null);
                                dialog.showAndWait()
                                        .ifPresent(name -> {
                                            modifiableBook.rename(name);
                                            bookName.setText(name);
                                        });
                            });
                            delete.setOnAction(e -> {
                                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to remove test '" + book.getName() + '\'');
                                confirm.setHeaderText(null);
                                confirm.showAndWait()
                                        .ifPresent(b -> {
                                            if (b == ButtonType.OK)
                                                Book.removeTest(book.getName());
                                        });
                            });
                        }
                        else {
                            rename.setDisable(true);
                            delete.setDisable(true);
                        }
                        delete.setStyle("-fx-text-fill: red");
                        menu.getItems().addAll(rename, changePath, new SeparatorMenuItem(), delete);

                        EventHandler<? super MouseEvent> onClick = e -> {
                            if (e.getButton() != MouseButton.PRIMARY)
                                return;
                        };
                        bookPane.setOnMouseClicked(onClick);

                        bookPane.setOnContextMenuRequested(e -> {
                            Bounds bounds = bookPane.localToScreen(bookPane.getBoundsInLocal());
                            menu.show(bookPane, e.getX() + bounds.getMinX(), e.getY() + bounds.getMinY());
                            bookPane.setOnMouseClicked(e2 -> {
                                menu.hide();
                                bookPane.setOnMouseClicked(onClick);
                                onClick.handle(e2);
                            });
                            menu.setOnAutoHide(e2 -> bookPane.setOnMouseClicked(onClick));
                            e.consume();
                        });

                        AtomicBoolean menuButtonIsShowing = new AtomicBoolean(false);
                        menuButton.setOnAction(e -> {
                            if (menuButtonIsShowing.compareAndSet(false, true)) {
                                menu.show(menuButton, Side.RIGHT, 0, 0);
                                testListMenu.hide();
                            }
                            else
                                menu.hide();
                        });

                        menu.setOnHiding(e -> menuButtonIsShowing.set(false));

                        AnchorPane.setTopAnchor(menuButton, TEST_LIST_MARGIN);
                        AnchorPane.setRightAnchor(menuButton, TEST_LIST_MARGIN);
                        bookPane.getChildren().add(menuButton);

                        bookPane.borderProperty().bind(new ObjectBinding<>() {
                            { bind(ColorSchemeManager.getTitleProperty()); }

                            @Override
                            protected Border computeValue() {
                                return new Border(new BorderStroke(ColorSchemeManager.getTitleProperty().get(), BorderStrokeStyle.DOTTED, CornerRadii.EMPTY, new BorderWidths(0, 0, 1, 0)));
                            }
                        });

                        return bookPane;
                    })
                    .collect(Collectors.toList())
        );

        //stage.

        //final double FOLDER_IMG_SCALE = 0.6;
        StackPane chooserDisplayWrapper = new StackPane();
        VBox chooserDisplay = new VBox();
        chooserDisplayWrapper.getChildren().add(chooserDisplay);
        pane.add(chooserDisplayWrapper, 1, 1);
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
        chooserDisplayWrapper.setOnMouseClicked(e -> openProject(stage));

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

    private static void addTests(ActionEvent e) {
        // TODO
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText("It worked!");
        alert.showAndWait();
    }
}
