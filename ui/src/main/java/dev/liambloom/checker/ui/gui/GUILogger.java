package dev.liambloom.checker.ui.gui;

import dev.liambloom.util.StringUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;

public class GUILogger implements System.Logger {
    private final String name;

    public GUILogger(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLoggable(Level level) {
        return level.compareTo(System.Logger.Level.INFO) >= 0;
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        if (!isLoggable(level))
            return;

        Alert alert = new Alert(
            switch (level) {
                case ERROR -> Alert.AlertType.ERROR;
                case WARNING -> Alert.AlertType.WARNING;
                case INFO -> Alert.AlertType.INFORMATION;
                default -> null; // Unreachable
            }
        );
        alert.setTitle(StringUtils.convertCase(alert.getAlertType().name(), StringUtils.Case.TITLE));
        alert.setHeaderText(thrown == null ? null : msg);
        StringWriter sw = new StringWriter();
        if (thrown != null)  {
            thrown.printStackTrace(new PrintWriter(sw));
            TextArea textArea = new TextArea(sw.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setExpandableContent(textArea);
        }

        alert.show();
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        log(level, bundle, String.format(format, params), (Throwable) null);
    }
}
