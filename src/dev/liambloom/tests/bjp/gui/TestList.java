//package dev.liambloom.tests.bjp.gui;
//
//import dev.liambloom.tests.bjp.shared.Book;
//import dev.liambloom.tests.bjp.shared.ConsumerThrowsIOException;
//import dev.liambloom.tests.bjp.shared.ModifiableBook;
//import dev.liambloom.tests.bjp.shared.PathBook;
//import javafx.beans.binding.ObjectBinding;
//import javafx.event.EventHandler;
//import javafx.geometry.Bounds;
//import javafx.geometry.Insets;
//import javafx.geometry.Side;
//import javafx.scene.control.*;
//import javafx.scene.input.MouseButton;
//import javafx.scene.input.MouseEvent;
//import javafx.scene.layout.*;
//import javafx.scene.shape.Circle;
//import javafx.scene.text.Text;
//import javafx.stage.FileChooser;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class TestList {
//    private static final double TEST_LIST_MARGIN = 5.0;
//    public final VBox node = new VBox();
//    private final ToggleGroup toggleGroup = new ToggleGroup();
//    private final Map<Toggle, Book> testMap = new HashMap<>();
//
//    public TestList() {
//
//    }
//
//    public Book getSelectedTests() {
//        return testMap.get(toggleGroup.getSelectedToggle());
//    }
//
//    private Label testNode(Book book) {
//        // I think a grid would be better for this
//        Label label = new Label();
//        AnchorPane bookPane = new AnchorPane();
//        label.setGraphic(bookPane);
//        bookPane.minWidthProperty().bind(node.widthProperty());
//        bookPane.maxWidthProperty().bind(node.widthProperty());
//        RadioButton toggle = new RadioButton();
//        toggle.setToggleGroup(toggleGroup);
//        label.setLabelFor(toggle);
//        label.setOnMouseClicked(e -> {
//            if (e.getButton() == MouseButton.PRIMARY)
//                toggle.setSelected(true);
//        });
//        AnchorPane.setLeftAnchor(toggle, TEST_LIST_MARGIN);
//        AnchorPane.setTopAnchor(toggle, TEST_LIST_MARGIN);
//        bookPane.getChildren().add(toggle);
//        Label bookName = new Label(book.getName());
//        bookName.textFillProperty().bind(ColorSchemeManager.getTitleProperty());
//        bookName.maxWidthProperty().bind(sidebarWidth.subtract(TEST_LIST_MARGIN * 4).subtract(DOTS_PANE_SIZE * 2));
//        AnchorPane.setTopAnchor(bookName, TEST_LIST_MARGIN);
//        AnchorPane.setLeftAnchor(bookName, TEST_LIST_MARGIN * 2 + DOTS_PANE_SIZE);
//        bookPane.getChildren().add(bookName);
//        MenuItem changePath = new MenuItem("Change Path");
//        double pathHeight;
//        if (book instanceof PathBook pathBook) {
//            Label bookPath = new Label(pathBook.getPath().getParent() + File.separator);
//            Label bookFileName = new Label(pathBook.getPath().getFileName().toString());
//            bookPath.maxWidthProperty().bind(node.widthProperty().subtract(bookFileName.widthProperty()).subtract(TEST_LIST_MARGIN * 2));
//            bookPath.textFillProperty().bind(ColorSchemeManager.getSubTitleProperty());
//            bookFileName.setFont(bookPath.getFont());
//            bookFileName.textFillProperty().bind(ColorSchemeManager.getSubTitleProperty());
//            AnchorPane.setBottomAnchor(bookPath, TEST_LIST_MARGIN);
//            AnchorPane.setLeftAnchor(bookPath, TEST_LIST_MARGIN);
//            AnchorPane.setBottomAnchor(bookFileName, TEST_LIST_MARGIN);
//            AnchorPane.setRightAnchor(bookFileName, TEST_LIST_MARGIN); // TODO: better ellipsis positioning
//            //bookPath.widthProperty().addListener((a1, a2, a3) -> AnchorPane.setLeftAnchor(bookFileName, bookPath.getWidth() + TEST_LIST_MARGIN));
//            bookPane.getChildren().addAll(bookPath, bookFileName);
//
//            Text dummyPathText = new Text();
//            dummyPathText.setFont(bookPath.getFont());
//            pathHeight = dummyPathText.getBoundsInLocal().getHeight();
//
//            changePath.setOnAction(e -> {
//                FileChooser chooser = new FileChooser();
//                chooser.setTitle("Open Test");
//                if (Files.exists(pathBook.getPath()))
//                    chooser.setInitialDirectory(pathBook.getPath().getParent().toFile());
//                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
//                Optional.ofNullable(chooser.showOpenDialog(stage))
//                        .map(File::toPath)
//                        .ifPresent((ConsumerThrowsIOException<Path>) p -> {
//                            pathBook.setPath(p);
//                            bookPath.setText(p.getParent().toString() + File.separator);
//                            bookFileName.setText(p.getFileName().toString());
//                        });
//            });
//        }
//        else {
//            pathHeight = 0;
//            changePath.setDisable(true);
//        }
//        Text dummyTitleText = new Text();
//        dummyTitleText.setFont(bookName.getFont());
//        double height = TEST_LIST_MARGIN * (pathHeight == 0 ? 2 : 3) + dummyTitleText.getBoundsInLocal().getHeight() + pathHeight;
//        bookPane.setMinHeight(height);
//        bookPane.setMaxHeight(height);
//        Pane threeDots = new Pane();
//                    /*threeDots.setMinSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);
//                    threeDots.setMaxSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);*/
//        final double DOT_RADIUS = 1.2;
//        Circle topDot = new Circle(DOTS_PANE_SIZE / 2.0, 4, DOT_RADIUS);
//        topDot.fillProperty().bind(ColorSchemeManager.getTitleProperty());
//        Circle middleDot = new Circle(DOTS_PANE_SIZE / 2.0, DOTS_PANE_SIZE / 2.0, DOT_RADIUS);
//        middleDot.fillProperty().bind(ColorSchemeManager.getTitleProperty());
//        Circle bottomDot = new Circle(DOTS_PANE_SIZE / 2.0, 12, DOT_RADIUS);
//        bottomDot.fillProperty().bind(ColorSchemeManager.getTitleProperty());
//        threeDots.getChildren().addAll(topDot, middleDot, bottomDot);
//        Button menuButton = new Button();
//        menuButton.setGraphic(threeDots);
//        menuButton.setPadding(Insets.EMPTY);
//        //menuButton.setBackground(Background.EMPTY);
//        //menuButton.getItems().add(new MenuItem("Delete"));
//        menuButton.setMinSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);
//        menuButton.setMaxSize(DOTS_PANE_SIZE, DOTS_PANE_SIZE);
//        menuButton.backgroundProperty().bind(new ObjectBinding<>() {
//            { bind(ColorSchemeManager.getAltBackgroundHoverProperty(), menuButton.hoverProperty()); }
//
//            @Override
//            protected Background computeValue() {
//                return menuButton.isHover()
//                        ? new Background(new BackgroundFill(ColorSchemeManager.getAltBackgroundHoverProperty().get(), new CornerRadii(2), Insets.EMPTY))
//                        : Background.EMPTY;
//            }
//        });
//        ContextMenu menu = new ContextMenu();
//        MenuItem rename = new MenuItem("Rename");
//        MenuItem delete = new MenuItem("Delete");
//        if (book instanceof ModifiableBook modifiableBook) {
//            rename.setOnAction(e -> {
//                TextInputDialog dialog = new TextInputDialog(book.getName());
//                dialog.setHeaderText(null);
//                dialog.setTitle("Rename Test");
//                dialog.showAndWait()
//                        .ifPresent(name -> {
//                            modifiableBook.rename(name);
//                            bookName.setText(name);
//                        });
//            });
//            delete.setOnAction(e -> {
//                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to remove test \"" + book.getName() + '"');
//                confirm.setHeaderText(null);
//                confirm.showAndWait()
//                        .ifPresent(b -> {
//                            if (b == ButtonType.OK)
//                                Book.removeTest(book.getName());
//                        });
//            });
//        }
//        else {
//            rename.setDisable(true);
//            delete.setDisable(true);
//        }
//        delete.setStyle("-fx-text-fill: red");
//        menu.getItems().addAll(rename, changePath, new SeparatorMenuItem(), delete);
//
//        EventHandler<? super MouseEvent> onClick = e -> {
//            if (e.getButton() != MouseButton.PRIMARY)
//                return;
//        };
//        bookPane.setOnMouseClicked(onClick);
//
//        bookPane.setOnContextMenuRequested(e -> {
//            Bounds bounds = bookPane.localToScreen(bookPane.getBoundsInLocal());
//            menu.show(bookPane, e.getX() + bounds.getMinX(), e.getY() + bounds.getMinY());
//            bookPane.setOnMouseClicked(e2 -> {
//                menu.hide();
//                bookPane.setOnMouseClicked(onClick);
//                onClick.handle(e2);
//            });
//            menu.setOnAutoHide(e2 -> bookPane.setOnMouseClicked(onClick));
//            e.consume();
//        });
//
//        AtomicBoolean menuButtonIsShowing = new AtomicBoolean(false);
//        menuButton.setOnAction(e -> {
//            if (menuButtonIsShowing.compareAndSet(false, true)) {
//                menu.show(menuButton, Side.RIGHT, 0, 0);
//                testListMenu.hide();
//            }
//            else
//                menu.hide();
//        });
//
//        menu.setOnHiding(e -> menuButtonIsShowing.set(false));
//
//        AnchorPane.setTopAnchor(menuButton, TEST_LIST_MARGIN);
//        AnchorPane.setRightAnchor(menuButton, TEST_LIST_MARGIN);
//        bookPane.getChildren().add(menuButton);
//
//        bookPane.borderProperty().bind(new ObjectBinding<>() {
//            { bind(ColorSchemeManager.getTitleProperty()); }
//
//            @Override
//            protected Border computeValue() {
//                return new Border(new BorderStroke(ColorSchemeManager.getTitleProperty().get(), BorderStrokeStyle.DOTTED, CornerRadii.EMPTY, new BorderWidths(0, 0, 1, 0)));
//            }
//        });
//
//        return label;
//    }
//}
