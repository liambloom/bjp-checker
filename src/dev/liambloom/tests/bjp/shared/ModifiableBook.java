package dev.liambloom.tests.bjp.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public abstract class ModifiableBook extends Book {
    public ModifiableBook(String name) {
        super(name);
    }

    public void setName(String name) {
        renameLoadedTest(getName(), name);
        Book.getCustomTests().put(name, Book.getCustomTests().get(this.name, null));
        Book.getCustomTests().remove(this.name);
        Preferences index = Book.getCustomTests().node("index");
        int size = index.getInt("size", 0);
        for (int i = 0; i < size; i++) {
            if (index.get(Integer.toString(i), null).equals(this.name)) {
                index.put(Integer.toString(i), name);
                this.name = name;
                return;
            }
        }
        throw new IllegalStateException("Book not found in index");
    }

    public abstract void addWatcher(Consumer<WatchEvent<Path>> cb) throws IOException;
}
