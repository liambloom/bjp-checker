package dev.liambloom.checker.ui;

import dev.liambloom.util.XMLUtils;
import dev.liambloom.util.function.FunctionUtils;
import dev.liambloom.util.function.PredicateThrowsException;
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class ResourceManager<T extends ResourceManager<T>.Resource> extends AbstractList<T> {
    private static final SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.0");
    private static final TransformerFactory tf = TransformerFactory.newInstance();
    private static final Path userDataDir = Path.of(AppDirsFactory.getInstance().getUserDataDir("Checker", null, null, true));
    private static final Path globalUpdatesDir = userDataDir.resolve("updates");
    private static final Exception versionValidationException;
    private final ArrayList<T> list = new ArrayList<>();
    private final Map<String, T> nameMap = new HashMap<>();
    private final Map<UUID, T> idMap = new HashMap<>();
    private final Path resourceDir;
    private final Path resourceUpdatesDir;
    protected final Document document;
    private final Transformer transformer;
    private final Path resourceListFile;
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
            String appVersionString = ResourceManager.class.getPackage().getImplementationVersion();
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
        catch (IOException | IncompatibleSettingsVersionException e) {
            localVersionValidationException = e;
        }

        versionValidationException = localVersionValidationException;
    }

    ResourceManager(String single) throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
        this(single, single + "s");
    }

    ResourceManager(String single, String plural) throws IOException, SAXException, TransformerException, IncompatibleSettingsVersionException {
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
        resourceDir = userDataDir.resolve(plural);
        resourceUpdatesDir = globalUpdatesDir.resolve(plural);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;

        try {
            synchronized (sf) {
                dbf.setSchema(sf.newSchema(
                    new StreamSource(ResourceManager.class.getResourceAsStream("/settings/schemas/" + plural + ".xsd"))));
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

        resourceListFile = userDataDir.resolve(plural + ".xml");

        if (Files.exists(resourceListFile)) {
            document = db.parse(Files.newInputStream(resourceListFile));
        }
        else {
            document = db.newDocument();
            document.appendChild(document.createElement(plural));
            save();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                save();
            }
            catch (IOException | TransformerException e) {
                System.getLogger(System.identityHashCode(ResourceManager.this) + "-shutdown").log(System.Logger.Level.ERROR,
                    "Failed to save some changes to " + plural, e);
            }
        }));
    }

    protected Element getElement(T t) {
        Element e = document.createElement(single);
        Element name = document.createElement("name");
        name.setTextContent(t.getName());
        e.appendChild(name);
        Element id = document.createElement("id");
        id.setTextContent(t.getId().toString());
        e.appendChild(id);
        Element digestElement = document.createElement("digest");
        digestElement.setTextContent(Base64.getEncoder().encodeToString(t.getExpectedDigest()));
        digestElement.setAttribute("algorithm", t.getDigestAlgorithm());
        e.appendChild(digestElement);
        Element sourceUrl = document.createElement("sourceUrl");
        sourceUrl.setTextContent(t.sourceUrl().toString());
        e.appendChild(sourceUrl);
        Element download = document.createElement("download");
        download.setTextContent(t.download() + "");
        e.appendChild(download);
        return e;
    }

    private T parseElement(Element e) {
        try {
            Element digestElement = (Element) e.getElementsByTagName("digest").item(0);
            return parseElement(new BaseResourceData(
                getTag(e, "name"),
                UUID.fromString(getTag(e, "id")),
                new Digest(digestElement.getAttribute("algorithm"), Base64.getDecoder().decode(digestElement.getTextContent())),
                new URL(getTag(e, "sourceUrl")),
                Boolean.parseBoolean(getTag(e, "download"))
                ), e);
        }
        catch (MalformedURLException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String getTag(Element e, String tag) {
        return e.getElementsByTagName(tag).item(0).getTextContent();
    }

    protected abstract T parseElement(BaseResourceData parsed, Element e); // this needs to handle digest and download and lots of other complicated stuff I should only write once

    public void save() throws IOException, TransformerException {
        DOMSource src = new DOMSource(document);
        StreamResult out = new StreamResult(Files.newOutputStream(resourceListFile));
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
                if (t.getName().equals(name))
                    return t;
            }

            return null;
        });
    }

    public T get(UUID id) {
        return idMap.computeIfAbsent(id, _key -> {
            for (T t : this) {
                if (t.getId().equals(id))
                    return t;
            }

            return null;
        });
    }

    @Override
    public int size() {
        return list.size();
    }


    protected void addResource(int i, T value) {
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

    @SuppressWarnings("RedundantThrows")
    public List<T> checkForUpdates() throws IOException {
        return list.stream()
            .filter(FunctionUtils.unchecked((PredicateThrowsException<T>) e -> e.updateAvailable(true)))
            .toList();
    }

    public int updateAll() throws IOException {
        int updated = 0;
        for (Resource r : list) {
            if (r.update())
                updated++;
        }
        return updated;
    }

    protected abstract class Resource {
        private String name;
        private final UUID id;
        private final String algorithm;
        private byte[] expectedDigest;
        private final Path resourcePath;
        private byte[] fileDigest;
        private final Path resourceUpdatePath;
        private URL sourceUrl;

        private byte[] sourceDigest;
        private boolean download;
        // add update() method, add fields for tracking if the downloaded file is problmeatic
        //  or if it neesd to be re-downloaed from the source. Also, *this* class should read
        //  the file, not the resource manager.

        protected Resource(String name, URL sourceUrl) {
            this(name, UUID.randomUUID(), null, sourceUrl, isNotLocalFile(sourceUrl));
        }

        protected static boolean isNotLocalFile(URL url) {
            try {
                return !url.getProtocol().equals("file") && (!url.getProtocol().equals("jar") || !((JarURLConnection) url.openConnection()).getJarFileURL().getProtocol().equals("file"));
            }
            catch (IOException e) { // I don't *think* this is ever thrown
                throw new UncheckedIOException(e);
            }
        }

        protected Resource(String name, UUID id, Digest digest, URL sourceUrl, boolean download) {
            this.name = name;
            this.id = id;
            this.sourceUrl = sourceUrl;
            this.download = download;
            String fileName = id + ".dat";
            this.resourcePath = resourceDir.resolve(fileName);
            resourceUpdatePath = resourceUpdatesDir.resolve(fileName);
            String algorithm = null;
            for (Digest d : new Digest[]{ digest, new Digest("SHA-512", null), new Digest("SHA-256", null) }) {
                if (d == null)
                    continue;
                try {
                    MessageDigest.getInstance(d.algorithm());
                    this.expectedDigest = d.digest();
                    algorithm = d.algorithm();
                    break;
                }
                catch (NoSuchAlgorithmException ignored) {

                }
            }
            assert algorithm != null;

            this.expectedDigest = null;
            this.algorithm = algorithm;
        }

        public String getName() {
            return name;
        }

        protected void setName(String value) {
            name = value;
        }

        public final UUID getId() {
            return id;
        }

        public byte[] getExpectedDigest() {
            return expectedDigest;
        }

        public String getDigestAlgorithm() {
            return algorithm;
        }

        private byte[] fileDigest(boolean fresh) throws IOException {
            if (!download)
                return sourceDigest(fresh);

            if (fileDigest == null || fresh) {
                try {
                    fileDigest = MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(resourcePath));
                }
                catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            return fileDigest;
        }

        public URL getSourceUrl() {
            return sourceUrl;
        }

        protected void setSourceUrl(URL value) throws IOException {
            this.sourceUrl = value;
            download = isNotLocalFile(value);
            this.updateAvailable(true);
            this.update();
        }

        private byte[] sourceDigest(boolean fresh) throws IOException {
            if (sourceDigest == null || fresh) {
                try (InputStream source = sourceUrl.openStream()) {
                    MessageDigest digest = MessageDigest.getInstance(algorithm);
                    Files.copy(new DigestInputStream(source, digest), resourceUpdatePath);
                    sourceDigest = digest.digest();
                }
                catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            return sourceDigest;
        }

        public final boolean isFileValid() throws IOException {
            return Arrays.equals(expectedDigest, fileDigest(!download));
        }

        public final boolean updateAvailable(boolean forceCheck) throws IOException {
            return Arrays.equals(expectedDigest, sourceDigest(forceCheck));
        }

        public final boolean update() throws IOException {
            if (!updateAvailable(false))
                return false;

            Files.copy(resourceUpdatePath, resourcePath);
            expectedDigest = sourceDigest;
            fileDigest = sourceDigest;
            return true;
        }

        public final URL sourceUrl() {
            return sourceUrl;
        }

        public final boolean download() {
            return download;
        }
    }

    public record BaseResourceData(String name, UUID id, Digest digest, URL sourceUrl, boolean download) { }
}
