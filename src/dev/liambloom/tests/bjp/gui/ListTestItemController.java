package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.Book;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class ListTestItemController {
    public RadioButton toggle;
    private VBox listNode;
    private Book book;
    private ToggleGroup toggleGroup = new ToggleGroup();
//    public static final double TEST_LIST_MARGIN = MainController.TEST_LIST_MARGIN;

    public VBox getListNode() {
        return listNode;
    }

    public void setListNode(VBox listNode) {
        this.listNode = listNode;
    }

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

    public void select(MouseEvent e) {
        toggle.setSelected(true);
    }
}
