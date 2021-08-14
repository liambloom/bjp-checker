package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.Book;
import dev.liambloom.tests.bjp.shared.ConsumerThrowsIOException;
import dev.liambloom.tests.bjp.shared.ModifiableBook;
import dev.liambloom.tests.bjp.shared.PathBook;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ListTestItemController {
    public RadioButton toggle;
    public AnchorPane node;
    public Button menuButton;
    private ContextMenu contextMenu;
    private ContextMenu testListMenu;
    private boolean isContextMenuButtonActive = false;
    //public AnchorPane node;
    //private ObjectProperty<VBox> listNode = new SimpleObjectProperty<>(new VBox());
    private Book book;
    private ToggleGroup toggleGroup = new ToggleGroup();
    public final BooleanProperty isValid = new SimpleBooleanProperty(true);
    public final DoubleProperty sidebarWidth = new SimpleDoubleProperty(MainController.INITIAL_SIDEBAR_WIDTH);
    public final DoubleBinding nameWidth = new DoubleBinding() {
        { bind(sidebarWidth, isValid); }

        @Override
        protected double computeValue() {
            return sidebarWidth.get() - (isValid.get() ? 52 : 73);
        }
    };
//    public static final double TEST_LIST_MARGIN = MainController.TEST_LIST_MARGIN;

//    public VBox getListNode() {
//        return listNode.get();
//    }
//
//    public void setListNode(VBox listNode) {
//        this.listNode.set(listNode);
//    }
//
//    public ObjectProperty<VBox> listNodeProperty() {
//        return listNode;
//    }

    /*public VBox getParent() {
        return (VBox) node.getParent();
    }

    public ReadOnlyObjectProperty<VBox> parentProperty() {
        return new ReadOnlyObjectWrapper<>(getParent());
    }*/

    public Book getBook() {
        return book;
    }

    public RadioButton getToggle() {
        return toggle;
    }

    public void setBook(Book book) {
        if (book instanceof ModifiableBook modifiableBook1) {
            this.book = new BeanBook(modifiableBook1);

        }
        else
            this.book = book;
        contextMenu = new ContextMenu();
        contextMenu.setOnAutoHide(e -> {
            isContextMenuButtonActive = false;
        });
        MenuItem rename = new MenuItem("Rename");
        MenuItem changePath = new MenuItem("Change Path");
        MenuItem delete = new MenuItem("Delete");
        if (book instanceof PathBook pathBook) {
            changePath.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Open Test");
                if (Files.exists(pathBook.getPath()))
                    chooser.setInitialDirectory(pathBook.getPath().getParent().toFile());
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                Optional.ofNullable(chooser.showOpenDialog(node.getScene().getWindow()))
                        .map(File::toPath)
                        .ifPresent((ConsumerThrowsIOException<Path>) pathBook::setPath);
                // bookPath.setText(p.getParent().toString() + File.separator);
                // bookFileName.setText(p.getFileName().toString());
            });
        }
        else {
            changePath.setDisable(true);
        }
        System.out.println(book.getName() + " " + (book instanceof ModifiableBook));
        if (this.book instanceof ModifiableBook modifiableBook) {
            rename.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(book.getName());
                dialog.setHeaderText(null);
                dialog.setTitle("Rename Test");
                dialog.showAndWait()
                        .ifPresent(modifiableBook::setName);
            });
            delete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to remove test \"" + book.getName() + '"');
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
        contextMenu.getItems().addAll(rename, changePath, new SeparatorMenuItem(), delete);
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroup;
    }

    public void setToggleGroup(ToggleGroup toggleGroup) {
        this.toggleGroup = toggleGroup;
    }

    public double getSidebarWidth() {
        return sidebarWidth.get();
    }

    public void setSidebarWidth(double value) {
        sidebarWidth.set(value);
    }

    public DoubleProperty sidebarWidthProperty() {
        return sidebarWidth;
    }

    public DoubleBinding nameWidthProperty() {
        return nameWidth;
    }

    public double getNameWidth() {
        return nameWidth.get();
    }

    public void setTestListMenu(ContextMenu value) {
        testListMenu = value;
    }

    public void select(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY)
            return;
        toggle.setSelected(true);
        toggle.requestFocus();
    }

    public void openContextMenuFromButton() {
        if (isContextMenuButtonActive)
            contextMenu.hide();
        else {
            contextMenu.show(menuButton, Side.RIGHT, 5, 0);
            testListMenu.hide();
        }
        isContextMenuButtonActive ^= true; // Inverts isContextMenuButtonActive
    }

    public void openContextMenuFromRequest(ContextMenuEvent e) {
        e.consume();
        Bounds bounds = node.localToScreen(node.getBoundsInLocal());
        contextMenu.show(node, e.getX() + bounds.getMinX(), e.getY() + bounds.getMinY());
    }

    public void initialize() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isValid.set(!isValid.get());
            }
        }).start();
    }
}
