package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class PathBook extends ModifiableBook {
    private Path path;

    PathBook(String name, Path path) {
        super(name);
        this.path = path;
    }

    @Override
    public Document getDocument(DocumentBuilder db) throws SAXException, IOException {
        if (exists())
            return db.parse(Files.newInputStream(path));
        else
            throw new NoSuchFileException(path.toString());
    }

    @Override
    public void addWatcher(Consumer<WatchEvent<Path>> cb) throws IOException {
        Book.addWatcher(path, cb);
    }

    @Override
    public boolean exists() throws IOException {
        try {
            return path.toRealPath().toString().endsWith(".xml") && super.exists();
        }
        catch (NoSuchFileException e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) throws IOException {
        this.path = path;
        setPath(getName(), path);
    }

    protected static void setPath(String name, Path path) throws IOException {
        if (!Files.exists(path) || !path.toRealPath().toString().endsWith(".xml"))
            throw new UserErrorException("Path `" + path + "' is not xml");
        Book.getCustomTests().put(name, path.toString());
    }

    @Override
    protected Source getSource() throws IOException {
        return new StreamSource(new BufferedInputStream(Files.newInputStream(path)));
    }
}
