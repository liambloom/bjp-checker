package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.Book;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class ListTestItemController {
    public RadioButton toggle;
    //public AnchorPane node;
    private ObjectProperty<VBox> listNode = new SimpleObjectProperty<>(new VBox());
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

    public VBox getListNode() {
        return listNode.get();
    }

    public void setListNode(VBox listNode) {
        this.listNode.set(listNode);
    }

    public ObjectProperty<VBox> listNodeProperty() {
        return listNode;
    }

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
        this.book = book;
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroup;
    }

    public void setToggleGroup(ToggleGroup toggleGroup) {
        this.toggleGroup = toggleGroup;
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

    public void select(MouseEvent e) {
        toggle.setSelected(true);
        toggle.requestFocus();
    }

    public void openContextMenu() {

    }

    public void openContextMenuAndConsume(ContextMenuEvent e) {
        e.consume();
        openContextMenu();
    }

    //{ System.out.println(getNameWidth()); }

    public void initialize() {
        Platform.runLater(() -> {
            System.out.println(getListNode().getWidth());
//            System.out.println(node.getMinWidth());
//            System.out.println(node.getMaxWidth());
//            System.out.println(node.getWidth());
        });
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
