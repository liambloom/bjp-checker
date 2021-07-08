package dev.liambloom.tests.book.bjp.checker;

import javax.swing.*;
import java.io.IOException;

public class GUIWrapper {
    public static void main(String[] args) {
        App.cleanArgs(args);
        try {
            //GUIWrapper.class.getClassLoader().loadClass("javafx.application.Application");
            javafx.application.Application.launch(GUI.class, args);
        } catch (NoClassDefFoundError e) {
            //Dialog dialog = new Dialog((Dialog) null, "Error");
            //dialog.a
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e2) {
                try {
                    App.createLogFile(e2);
                } catch (IOException ignored) { }
            }
            JOptionPane.showMessageDialog(null, "Please make sure that JavaFX is installed and in the classpath", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
