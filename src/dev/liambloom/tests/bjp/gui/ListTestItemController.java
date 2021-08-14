package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.*;
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
import java.io.IOException;
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
    private final ObjectProperty<BeanBook> book = new SimpleObjectProperty<>();
    private ToggleGroup toggleGroup = new ToggleGroup();
    public final DoubleProperty sidebarWidth = new SimpleDoubleProperty(MainController.INITIAL_SIDEBAR_WIDTH);
    private final ReadOnlyDoubleWrapper nameWidthWrapper = new ReadOnlyDoubleWrapper(0.0);
    public final ReadOnlyDoubleProperty nameWidth = nameWidthWrapper.getReadOnlyProperty();
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

//    public Book getBook() {
//        return book.getInner();
//    }

    public RadioButton getToggle() {
        return toggle;
    }

    public BeanBook getBook() {
        return book.get();
    }

    public void setBook(Book book) throws IOException {
        this.book.set(new BeanBook(book));
        nameWidthWrapper.bind(new DoubleBinding() {
            { bind(sidebarWidth, ListTestItemController.this.book.get().validationResultProperty()); }

            @Override
            protected double computeValue() {
                return sidebarWidth.get() - (ListTestItemController.this.book.get().getValidationResult().status() == TestValidationStatus.VALID ? 52 : 73);
            }
        });
        contextMenu = new ContextMenu();
        contextMenu.setOnAutoHide(e -> isContextMenuButtonActive = false);
        MenuItem rename = new MenuItem("Rename");
        MenuItem changePath = new MenuItem("Change Path");
        MenuItem delete = new MenuItem("Delete");
        if (this.book.get().hasPath()) {
            assert this.book.get().getPath().isPresent();
            changePath.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Open Test");
                if (Files.exists(this.book.get().getPath().get()))
                    chooser.setInitialDirectory(this.book.get().getPath().get().getParent().toFile());
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                Optional.ofNullable(chooser.showOpenDialog(node.getScene().getWindow()))
                        .map(File::toPath)
                        .ifPresent((ConsumerThrowsIOException<Path>) this.book.get()::setPath);
                // bookPath.setText(p.getParent().toString() + File.separator);
                // bookFileName.setText(p.getFileName().toString());
            });
        }
        else {
            changePath.setDisable(true);
        }
        if (this.book.get().isModifiable()) {
            rename.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(this.book.get().getName());
                dialog.setHeaderText(null);
                dialog.setTitle("Rename Test");
                dialog.showAndWait()
                        .ifPresent(this.book.get()::setName);
            });
            delete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to remove test \"" + this.book.get().getName() + '"');
                confirm.setHeaderText(null);
                confirm.showAndWait()
                        .ifPresent(b -> {
                            if (b == ButtonType.OK)
                                Book.removeTest(this.book.get().getName());
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

    public ObjectProperty<BeanBook> bookProperty() {
        return book;
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

    public ReadOnlyDoubleProperty nameWidthProperty() {
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
        /*new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isValid.set(!isValid.get());
            }
        }).start();*/
    }
}
