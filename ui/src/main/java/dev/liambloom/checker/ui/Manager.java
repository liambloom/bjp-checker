package dev.liambloom.checker.ui;

import dev.liambloom.util.XMLUtils;
import net.harawata.appdirs.AppDirsFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class Manager<T extends Managed> extends AbstractList<T> {
    private static final SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.0");
    private static final TransformerFactory tf = TransformerFactory.newInstance();
    private static final Path dir = Path.of(AppDirsFactory.getInstance().getUserDataDir("Checker", null, null, true));
    private static final Exception versionValidationException;
    private final ArrayList<T> list = new ArrayList<>();
    private final Map<String, T> nameMap = new HashMap<>();
    private final Map<UUID, T> idMap = new HashMap<>();
    private final Document document;
    private final Transformer transformer;
    private final Path path;
    private final String single;
    private final String plural;

    static {
        try {
            sf.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
        }
        catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }

        Exception localVersionValidationException = null;
        try {
            String appVersionString = Manager.class.getPackage().getImplementationVersion();
            int[] versions = Arrays.stream(appVersionString.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
            ByteBuffer appVersion = ByteBuffer.allocate(versions.length * 4);
            for (int version : versions)
                appVersion.putInt(version);
            Path versionFile = dir.resolve("version.dat");
            if (Files.exists(versionFile)) {
                ByteBuffer settingsVersion = ByteBuffer.wrap(Files.readAllBytes(versionFile));

                if (appVersion.compareTo(settingsVersion) > 0) {
                    throw IncompatibleSettingsVersionException.cantUpgrade(settingsVersion);
                }
                else if (appVersion.compareTo(settingsVersion) < 0) {
                    throw IncompatibleSettingsVersionException.cantDowngrade(settingsVersion);
                }
            }
            else {
                Files.createFile(versionFile);
                Files.write(versionFile, appVersion.array());
            }
        }
        catch (IOException | IncompatibleSettingsVersionException e) {
            localVersionValidationException = e;
        }

        versionValidationException = localVersionValidationException;
    }

    Manager(String single) throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
        this(single, single + "s");
    }

    Manager(String single, String plural) throws IOException, SAXException, TransformerException, IncompatibleSettingsVersionException {
        if (versionValidationException != null) {
            if (versionValidationException instanceof IOException e)
                throw e;
            else if (versionValidationException instanceof IncompatibleSettingsVersionException e)
                throw e;
            else
                throw new IllegalStateException("Version validation threw unexpected exception", versionValidationException);
        }

        this.single = single;
        this.plural = plural;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;

        try {
            synchronized (sf) {
                dbf.setSchema(sf.newSchema(
                    new StreamSource(Manager.class.getResourceAsStream("/settings/schemas/" + plural + ".xsd"))));
            }
            dbf.setNamespaceAware(true);
            db = dbf.newDocumentBuilder();

            synchronized (tf) {
                transformer = tf.newTransformer();
            }
        }
        catch (SAXException | ParserConfigurationException | TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        path = dir.resolve(plural + ".xml");

        if (Files.exists(path)) {
            document = db.parse(Files.newInputStream(path));
        }
        else {
            document = db.newDocument();
            document.appendChild(document.createElement(plural));
            save();
        }
    }

    private Element getElement(T t) {
        // generate element using Manged#fields
    }

    protected abstract T parseElement(Element e); // this needs to handle digest and download and lots of other complicated stuff I should only write once

    public void save() throws IOException, TransformerException {
        DOMSource src = new DOMSource(document);
        StreamResult out = new StreamResult(Files.newOutputStream(path));
        transformer.transform(src, out);
    }

    private Element getElementAt(int i) {
        return XMLUtils.streamNodeListElements(document.getChildNodes()).skip(i).findFirst().orElseThrow();
    }

    @Override
    public T get(int i) {
        if (i < size()) {
            T t = list.get(i);
            if (t != null)
                return t;
        }

        list.ensureCapacity(i);
        for (int j = size(); j < i; j++)
            list.add(null);

        T e = parseElement(getElementAt(i));
        list.add(e);
        return e;
    }

    public T get(String name) {
        return nameMap.computeIfAbsent(name, _key -> {
            for (T t : this) {
                if (t.name().equals(name))
                    return t;
            }

            return null;
        });
    }

    public T get(UUID id) {
        return idMap.computeIfAbsent(id, _key -> {
            for (T t : this) {
                if (t.id().equals(id))
                    return t;
            }

            return null;
        });
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public T set(int i, T value) {
        Element e = getElement(value);
        document.replaceChild(e, getElementAt(i));
        return list.set(i, value);
    }

    @Override
    public void add(int i, T value) {
        Element e = getElement(value);
        if (i == size())
            document.appendChild(e);
        else
            document.insertBefore(e, getElementAt(i));
        list.add(i, value);
    }

    @Override
    public T remove(int i) {
        document.removeChild(getElementAt(i));
        return list.remove(i);
    }
}
