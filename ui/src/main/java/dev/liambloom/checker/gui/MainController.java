package dev.liambloom.checker.gui;

import dev.liambloom.checker.book.Book;
import dev.liambloom.checker.uiShared.Books;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MainController {
    public static final double INITIAL_SIDEBAR_WIDTH = 200;
    public final DoubleProperty sidebarWidth = new SimpleDoubleProperty(INITIAL_SIDEBAR_WIDTH);
    public final ContextMenu testListMenu;
    public GridPane root;
    public VBox testList;
    public ToggleGroup testToggleGroup = new ToggleGroup();
    // public static final double TEST_LIST_MARGIN = 5.0;
    // public static final double LIST_BUTTON_SIZE = 16;

    //public double getTestListMargin()

    public DoubleProperty sidebarWidthProperty() {
        return sidebarWidth;
    }

    public double getSidebarWidth() {
        return sidebarWidth.get();
    }

    public void setSidebarWidth(double value) {
        sidebarWidth.set(value);
    }

    public final StringProperty themeName = new SimpleStringProperty("light-theme");

    public DoubleProperty themeNameProperty() {
        return sidebarWidth;
    }

    public double getThemeName() {
        return sidebarWidth.get();
    }

    public void setThemeName(double value) {
        sidebarWidth.set(value);
    }

    public MainController() {
        testListMenu = new ContextMenu();
        MenuItem addTestMenuItem = new MenuItem("Add Test");
        addTestMenuItem.setOnAction(this::addTests);
        testListMenu.getItems().add(addTestMenuItem);
    }

    @FXML
    private void addTests(ActionEvent e) {
        // TODO: Should this use FXML?
        // https://stackoverflow.com/a/42444420/11326662
        Dialog<Pair<String, File>> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setHeaderText(null);
        dialog.setTitle("New Test");
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        TextField name = new TextField();
        name.setPromptText("name");
        pane.add(name, 0, 0);
        Button file = new Button("File");
        AtomicReference<File> fileInputValue = new AtomicReference<>();
        file.setOnAction(e2 -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Test File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML File", "*.xml"));
            Optional.ofNullable(fileInputValue.get())
                .map(File::getParentFile)
                .ifPresent(chooser::setInitialDirectory);
            File f = chooser.showOpenDialog(dialog.getOwner());
            if (f != null)
                fileInputValue.set(f);
        });
        pane.add(file, 0, 1);
        dialog.getDialogPane().setContent(pane);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new Pair<>(name.getText(), fileInputValue.get());
            }
            else
                return null;
        });

        dialog.showAndWait()
            .ifPresent((ConsumerThrowsIOException<Pair<String, File>>) (pair -> {
                Books.addBook(pair.getKey(), pair.getValue().toPath());
                addTests(Books.getBook(pair.getKey()));
            }));
    }

    private void addTests(Book book) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/TestListItem.fxml"));
//        loader.getNamespace().put("TEST_LIST_MARGIN", TEST_LIST_MARGIN);
        Parent node = loader.load();
        if (testList.getChildren().size() == 1)
            node.setId("firstTestListItem");
        TestListItemController controller = loader.getController();
//        controller.setListNode(testList);
        controller.setToggleGroup(testToggleGroup);
        controller.setBook(book);
        controller.setTestListMenu(testListMenu);
        controller.sidebarWidthProperty().bind(sidebarWidth);
//        if (testToggleGroup.getSelectedToggle() == null)
//            controller.toggle.setSelected(true);
        testList.getChildren().add(node);
    }

    public void initialize() throws IOException {
        try {
            Books.getAllBooks()
                .forEachOrdered((ConsumerThrowsIOException<Book>) this::addTests);
            if (testToggleGroup.getSelectedToggle() == null || ((Control) testToggleGroup.getSelectedToggle()).isDisabled())
                ((Toggle) ((Parent) testList.getChildren().get(1)).getChildrenUnmodifiable().get(0)).setSelected(true);
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public void openListContextMenu(ContextMenuEvent e) {
        Bounds bounds = testList.localToScreen(testList.getBoundsInLocal());
        testListMenu.show(testList, e.getX() + bounds.getMinX(), e.getY() + bounds.getMinY());
        testList.setOnMouseClicked(e2 -> {
            testListMenu.hide();
        });
        testListMenu.setOnHiding(e2 -> {
            testList.setOnMouseClicked(null);
            testListMenu.setOnHiding(null);
        });
    }
}
