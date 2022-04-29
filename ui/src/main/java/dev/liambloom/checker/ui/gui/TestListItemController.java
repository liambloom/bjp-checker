package dev.liambloom.checker.ui.gui;

import dev.liambloom.checker.BookLocator;
import dev.liambloom.checker.ui.SelfLoadingBook;
import dev.liambloom.checker.ui.Books;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.TestValidationStatus;
import dev.liambloom.util.function.FunctionUtils;
import dev.liambloom.util.StringUtils;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

public class TestListItemController {
    public RadioButton toggle;
    public AnchorPane node;
    public Button menuButton;
    public Pane iconPane;
    private ContextMenu contextMenu;
    private ContextMenu testListMenu;
    private boolean isContextMenuButtonActive = false;
    //public AnchorPane node;
    //private ObjectProperty<VBox> listNode = new SimpleObjectProperty<>(new VBox());
    private final ObjectProperty<SelfLoadingBook> book = new SimpleObjectProperty<>();
    private ToggleGroup toggleGroup = new ToggleGroup();
    public final DoubleProperty sidebarWidth = new SimpleDoubleProperty(MainController.INITIAL_SIDEBAR_WIDTH);
    private final ReadOnlyDoubleWrapper nameWidthWrapper = new ReadOnlyDoubleWrapper(0.0);
    public final ReadOnlyDoubleProperty nameWidth = nameWidthWrapper.getReadOnlyProperty();
    public final StringBinding icon = new StringBinding() {
        {
            bind(book);
            book.addListener((observable, oldValue, newValue) -> {
                if (oldValue != null)
                    unbind(oldValue.validationResultProperty());
                bind(newValue.validationResultProperty());
            });
        }

        @Override
        protected String computeValue() {
            return switch ((TestValidationStatus) Optional.ofNullable(book.get())
                .map(SelfLoadingBook::getValidationResult)
                .map(Result::status)
                .orElse(TestValidationStatus.NOT_FOUND)) {
                case VALID -> "";
                case NOT_FOUND -> "M 0 8 A 8 8 0 0 1 16 8 A 8 8 0 0 1 0 8 M 5.171572875 6 A 2.828427125 2.828427125 0 1 1 10 8 A 3.414213562 3.414213562 0 0 0 9 10.41421356 h -2 A 5.414213562 5.414213562 0 0 1 8.585786438 6.585786438 A 0.8284271247 0.8284271247 0 1 0 7.171572875 6 M 7 11.8 v 2 h 2 v -2 h -2";
                case INVALID, VALID_WITH_WARNINGS -> "M 0 14.92820323 L 8 1.07179677 L 16 14.92820323 M 7 4.8 V 8 L 7.5 10 h 1 L 9 8 V 4.8 H 7 M 7 11 v 2 h 2 v -2 h -2";
            };
        }
    };
    // This doesn't seem like it should be necessary, but I couldn't find another way
    public final ObjectBinding<Color> iconFill = new ObjectBinding<>() {
        {
            bind(book);
            book.addListener((observable, oldValue, newValue) -> {
                if (oldValue != null)
                    unbind(oldValue.validationResultProperty());
                bind(newValue.validationResultProperty());
            });
        }

        @Override
        protected Color computeValue() {
            return Optional.ofNullable(book.get())
                .map(SelfLoadingBook::getValidationResult)
                .map(Result::status)
                .orElse(TestValidationStatus.NOT_FOUND)
                .color()
                .jfx();
        }
    };
    /*public final ObjectBinding<Color> iconColor = new ObjectBinding<>() {
        {
            bind(book);
            book.addListener((observable, oldValue, newValue) -> {
                if (oldValue != null)
                    unbind(oldValue.validationResultProperty());
                bind(newValue.validationResultProperty());
            });
        }

        @Override
        protected Color computeValue() {
            return book.get().getValidationResult().status().color().getJavaFX();
        }
    };*/
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

    public SelfLoadingBook getBook() {
        return book.get();
    }

    public void setBook(BookLocator book) throws IOException {
        this.book.set(new SelfLoadingBook(book));
        nameWidthWrapper.bind(new DoubleBinding() {
            {
                bind(sidebarWidth, TestListItemController.this.book.get().validationResultProperty());
            }

            @Override
            protected double computeValue() {
                return sidebarWidth.get() - (TestListItemController.this.book.get().getValidationResult().status() == TestValidationStatus.VALID ? 52 : 73);
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
                Path initialDir = this.book.get().getPath().get().getParent();
                if (Files.exists(initialDir))
                    chooser.setInitialDirectory(initialDir.toFile());
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                Optional.ofNullable(chooser.showOpenDialog(node.getScene().getWindow()))
                    .map(File::toPath)
                    .ifPresent(FunctionUtils.unchecked(this.book.get()::setPath));
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
                            Books.removeBook(this.book.get().getName());
                    });
            });
        }
        else {
            rename.setDisable(true);
            delete.setDisable(true);
        }
        delete.setStyle("-fx-text-fill: red");
        contextMenu.getItems().addAll(rename, changePath, new SeparatorMenuItem(), delete);
//        System.out.println(getBook().getName());
//        Platform.runLater(() -> {
//            System.out.println(getBook().getName());
//            System.out.println(foo.getText());
//            System.out.println(foo.textProperty());
//        });
        toggle.disableProperty().bind(
            new BooleanBinding() {
                {
                    bind(TestListItemController.this.book, TestListItemController.this.getBook().validationResultProperty());
                    TestListItemController.this.book.addListener((observable, oldValue, newValue) -> {
                        if (oldValue != null)
                            unbind(oldValue.validationResultProperty());
                        bind(newValue.validationResultProperty());
                    });
                }

                @Override
                protected boolean computeValue() {
                    return Optional.ofNullable(TestListItemController.this.getBook())
                        .map(SelfLoadingBook::getValidationResult)
                        .map(Result::status)
                        .map(TestValidationStatus.class::cast)
                        .orElse(TestValidationStatus.NOT_FOUND)
                        .compareTo(TestValidationStatus.NOT_FOUND) >= 0;
                }
            }
        );
        toggle.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && toggle.isSelected()) {
                toggle.setSelected(false);
                ((Toggle) ((Parent) node.getParent().getChildrenUnmodifiable().get(1)).getChildrenUnmodifiable().get(0)).setSelected(true);
            }
        });

        if (getBook().getName().equals(Preferences.userRoot().node("dev/liambloom/checker").get("selectedTests", null)))
            toggle.setSelected(true);
    }

    public ObjectProperty<SelfLoadingBook> bookProperty() {
        return book;
    }

    public String getIcon() {
        return icon.get();
    }

    public StringBinding iconProperty() {
        return icon;
    }

    public Color getIconFill() {
        return iconFill.get();
    }

    public ObjectBinding<Color> iconFillProperty() {
        return iconFill;
    }

    /*public Color getIconColor() {
        return iconColor.get();
    }

    public ObjectBinding<Color> iconColorProperty() {
        return iconColor;
    }*/

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
        if (e.getButton() != MouseButton.PRIMARY || toggle.isDisabled())
            return;
        toggle.setSelected(true);
        toggle.requestFocus();
    }

    private void updateSelectedName(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        Preferences.userRoot().node("dev/liambloom/checker").put("selectedTests", newValue);
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
        Tooltip iconTooltip = new Tooltip();
        iconTooltip.textProperty().bind(new StringBinding() {
            {
                bind(book);
                book.addListener((observable, oldValue, newValue) -> {
                    if (oldValue != null)
                        unbind(oldValue.validationResultProperty());
                    bind(newValue.validationResultProperty());
                });
            }

            @Override
            protected String computeValue() {
                return getBook() == null ? null : StringUtils.convertCase(getBook().getValidationResult().status().toString(), StringUtils.Case.TITLE);
            }
        });
        Tooltip.install(iconPane, iconTooltip);

        toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Preferences.userRoot().node("dev/liambloom/checker").put("selectedTests", getBook().getName());
                getBook().nameProperty().addListener(this::updateSelectedName);
            }
            else
                getBook().nameProperty().removeListener(this::updateSelectedName);
        });
    }
}
