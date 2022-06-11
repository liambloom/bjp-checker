package dev.liambloom.checker.ui;

import dev.liambloom.util.function.FunctionUtils;
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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class PersistentData {
    protected static final Path userDataDir = Path.of(AppDirsFactory.getInstance().getUserDataDir("Checker", null, null, true));
    protected static final Path globalUpdatesDir = userDataDir.resolve("updates");
    protected static final TransformerFactory tf = TransformerFactory.newInstance();
    protected static final SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.0");
    private static final Exception versionValidationException;
    protected final AtomicBoolean changed = new AtomicBoolean();
    protected final String name;
    protected final Path dataFile;
    protected final Schema schema;
    protected final Document document;
    protected final Transformer transformer;
    private final Validator saveValidator;

    static {
        try {
            sf.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
        }
        catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }

        String appVersionString = PersistentData.class.getPackage().getImplementationVersion();
        Exception localVersionValidationException = null;
        if (appVersionString == null) {
            System.getLogger(PersistentData.class.getName()).log(System.Logger.Level.WARNING, "Unable to check settings version compatibility");
        }
        else {
            try {
                int[] versions = Arrays.stream(appVersionString.split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();
                ByteBuffer appVersion = ByteBuffer.allocate(versions.length * 4);
                for (int version : versions)
                    appVersion.putInt(version);
                Path versionFile = userDataDir.resolve("version.dat");
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
            catch(IOException | IncompatibleSettingsVersionException e){
                localVersionValidationException = e;
            }
        }

        versionValidationException = localVersionValidationException;
    }

    public PersistentData(String name) throws IOException, IncompatibleSettingsVersionException, TransformerException, SAXException {
        validateVersion();
        this.name = name;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;

        try {
            synchronized (sf) {
                schema = sf.newSchema(new StreamSource(ResourceManager.class.getResourceAsStream("/settings/schemas/" + name + ".xsd")));
            }
            saveValidator = schema.newValidator();
            dbf.setSchema(schema); // This line doesn't seem to...do anything?
            dbf.setNamespaceAware(true);
            db = dbf.newDocumentBuilder();

            synchronized (tf) {
                transformer = tf.newTransformer();
            }
        }
        catch (SAXException | ParserConfigurationException | TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        dataFile = userDataDir.resolve(name + ".xml");

        if (Files.exists(dataFile)) {
            document = db.parse(Files.newInputStream(dataFile));
        }
        else {
            document = db.newDocument();
            document.appendChild(document.createElement(name));
            Files.createDirectories(dataFile.getParent());
            Files.createFile(dataFile);
            save();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (changed.getAcquire())
                    save();
            }
            catch (IOException | TransformerException | SAXException e) {
                System.getLogger(PersistentData.class.getName() + System.identityHashCode(PersistentData.this) + "-shutdown").log(System.Logger.Level.ERROR,
                    "Failed to save some changes to " + name, e);
            }
        }));
    }

    protected static void validateVersion() throws IOException, IncompatibleSettingsVersionException {
        if (versionValidationException != null) {
            if (versionValidationException instanceof IOException e)
                throw e;
            else if (versionValidationException instanceof IncompatibleSettingsVersionException e)
                throw e;
            else
                throw new IllegalStateException("Version validation threw unexpected exception", versionValidationException);
        }
    }

    protected static String getTag(Element e, String tag) {
        return e.getElementsByTagName(tag).item(0).getTextContent();
    }

    public void save() throws IOException, TransformerException, SAXException {
        System.getLogger(System.identityHashCode(this) + "").log(System.Logger.Level.TRACE, "Saving " + name);
        DOMSource src = new DOMSource(document);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(src, new StreamResult(outputStream));
        byte[] buf = outputStream.toByteArray();
        // validating the DOMSource itself didn't work. IDK why. But this works, so I'm satisfied(ish)
        saveValidator.validate(new StreamSource(new ByteArrayInputStream(buf)));
        Files.write(dataFile, buf);
    }
}
