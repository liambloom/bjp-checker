package dev.liambloom.tests.bjp.checker;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class TextLine {
    private final String text;
    private final Color color;
    private final int indentLevel;

    public TextLine(String text, Color color, int indentLevel) {
        this.text = text;
        this.color = color;
        this.indentLevel = indentLevel;
    }

    public Text getJfxText() {
        Text text = new Text(this.text);
        text.setFill(color.jfx);
        VBox.setMargin(text, new Insets(0, 0, 0, indentLevel * 5));
        return text;
    }

    public String getTerminalText() {
        return " ".repeat(4 * indentLevel) + "\u001b[" + color.ansi + "m" + text + "\u001b[0m";
    }
}
