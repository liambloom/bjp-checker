package dev.liambloom.checker.ui;

import dev.liambloom.util.XMLUtils;
import dev.liambloom.util.function.FunctionUtils;
import dev.liambloom.util.function.PredicateThrowsException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public abstract class ResourceManager<T extends ResourceManager<T>.Resource> extends PersistentData implements Iterable<T> {
    private final ArrayList<T> list = new ArrayList<>();
    private final Map<String, T> nameMap = new HashMap<>();
    private final Map<UUID, T> idMap = new HashMap<>();
    private final Lock nameChangeLock = new ReentrantLock();
    private final String single;
    protected final Path resourceDir;
    protected final Path resourceUpdatesDir;
    protected final List<T> inner = new AbstractList<>() {
        @Override
        public T get(int i) {
            if (i >= size() || i < 0)
                throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + size());

            if (i < list.size()) {
                T t = list.get(i);
                if (t != null)
                    return t;
            }

            list.ensureCapacity(i);
            for (int j = size(); j < i; j++)
                list.add(null);

            T e = parseElement(getElementAt(i));
            e.addChangeListener(() -> changed.setRelease(true));
            list.add(e);
            return e;
        }

        @Override
        public void add(int i, T value) {
            changed.setRelease(true);

            try {
                value.update();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            value.addChangeListener(() -> changed.setRelease(true));

            Element e = getElement(value);

            nameChangeLock.lock();
            try {
                if (Stream.concat(
                    XMLUtils.streamNodeListElements(document.getElementsByTagName("name"))
                        .map(Element::getTextContent),
                    list.stream().map(T::getName))
                        .anyMatch(value.getName()::equals)) {
                    throw new IllegalArgumentException("Name unavailable");
                }
                else {
                    if (i == size())
                        document.getDocumentElement().appendChild(e);
                    else
                        document.getDocumentElement().insertBefore(e, getElementAt(i));

                    list.add(i, value);
                }
            }
            finally {
                nameChangeLock.unlock();
            }

        }

        @Override
        public T remove(int i) {
            changed.setRelease(true);
            T e = list.get(i);
            if (e != null) {
                try {
                    e.remove();
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            document.getDocumentElement().removeChild(getElementAt(i));
            return list.remove(i);
        }

        @Override
        public int size() {
            return document.getDocumentElement().getElementsByTagName(single).getLength(); //list.size();
        }
    };

    ResourceManager(String single) throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
        this(single, single + "s");
    }

    ResourceManager(String single, String plural) throws IOException, SAXException, TransformerException, IncompatibleSettingsVersionException {
        super(plural);

        this.single = single;
        resourceDir = userDataDir.resolve(plural);
        resourceUpdatesDir = globalUpdatesDir.resolve(plural);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (changed.getAcquire()) {
                    if (Files.exists(resourceUpdatesDir)) {
                        try (Stream<Path> update = Files.list(resourceUpdatesDir)) {
                            update.forEach(FunctionUtils.unchecked(Files::delete));
                        }
                        Files.delete(resourceUpdatesDir);
                    }
                }
            }
            catch (IOException e) {
                System.getLogger(ResourceManager.class.getName() + System.identityHashCode(this) + "-shutdown").log(System.Logger.Level.WARNING,
                    "Temporary " + single + " files not deleted successfully", e);
            }
        }));
    }

    public String getSingleName() {
        return single;
    }

    public String getPluralName() {
        return name;
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
        digestElement.setTextContent(Optional.ofNullable(t.getExpectedDigest()).map(Base64.getEncoder()::encodeToString).orElse(""));
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
            var digest = new Digest(digestElement.getAttribute("algorithm"), Base64.getDecoder().decode(digestElement.getTextContent()));
//            System.out.println("Saved:    " + Base64.getEncoder().encodeToString(digest.digest()));
//            System.out.println(getTag(e, "id"));
            //new Error().printStackTrace();
            return parseElement(new BaseResourceData(
                getTag(e, "name"),
                UUID.fromString(getTag(e, "id")),
                digest,
                new URL(getTag(e, "sourceUrl")),
                Boolean.parseBoolean(getTag(e, "download"))
                ), e);
        }
        catch (MalformedURLException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    protected abstract T parseElement(BaseResourceData parsed, Element e); // this needs to handle digest and download and lots of other complicated stuff I should only write once

    @Override
    public void save() throws IOException, TransformerException, SAXException {
        for (T e : list) {
            if (e == null)
                continue;
            e.save();
        }
        super.save();
    }

    private Element getElementAt(int i) {
        return XMLUtils.streamNodeListElements(document.getDocumentElement().getElementsByTagName(single)).skip(i).findFirst().orElseThrow();
    }

    public T get(int i) {
        return inner.get(i);
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

    public int size() {
        return inner.size();
    }

    public T remove(int index) throws IOException {
        try {
            return inner.remove(index);
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public boolean remove(Object o) throws IOException {
        try {
            return inner.remove(o);
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Iterator<T> iterator() {
        return inner.iterator();
    }

    public Stream<T> stream() {
        return inner.stream();
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

    public abstract class Resource {
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
        private final List<ResourceEventListener> changeListeners = Collections.synchronizedList(new ArrayList<>());
        private final List<ResourceEventListener> removalListeners = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean removed = new AtomicBoolean(false);
        private final AtomicBoolean saved = new AtomicBoolean(true);
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
            this.resourcePath = download ? resourceDir.resolve(fileName) : null;
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

            this.algorithm = algorithm;
        }

        public String getName() {
            checkRemoved();
            return name;
        }

        /**
         * Changes the name of the resource
         *
         * @param value The new name
         * @throws NullPointerException if {@code value} is {@code null}
         * @throws IllegalArgumentException if the name is already taken
         */
        protected void setName(String value) {
            checkRemoved();
            nameChangeLock.lock();
            try {
                if (ResourceManager.this.get(Objects.requireNonNull(value)) != null)
                    throw new IllegalArgumentException("Name unavailable");
                name = value;
            }
            finally {
                nameChangeLock.unlock();
            }
            changed();
        }

        public final UUID getId() {
            checkRemoved();
            return id;
        }

        public byte[] getExpectedDigest() {
            checkRemoved();
            return expectedDigest;
        }

        public String getDigestAlgorithm() {
            checkRemoved();
            return algorithm;
        }

        private byte[] fileDigest(boolean fresh) throws IOException {
            checkRemoved();
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
            checkRemoved();
            return sourceUrl;
        }

        public void setSourceUrl(URL value) throws IOException {
            checkRemoved();
            this.sourceUrl = Objects.requireNonNull(value);
            download = isNotLocalFile(value);
            this.updateAvailable(true);
            if (this.update())
                changed();
        }

        private byte[] sourceDigest(boolean fresh) throws IOException {
            checkRemoved();
            if (sourceDigest == null || fresh) {
                try (InputStream source = sourceUrl.openStream()) {
                    MessageDigest digest = MessageDigest.getInstance(algorithm);
                    Files.createDirectories(resourceUpdatesDir);
                    Files.copy(new DigestInputStream(source, digest), resourceUpdatePath, StandardCopyOption.REPLACE_EXISTING);
                    sourceDigest = digest.digest();
                }
                catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                catch (FileNotFoundException e) {
                    sourceDigest = null;
                    throw e;
                }
                finally {
                    if (sourceDigest == null || !Arrays.equals(sourceDigest, expectedDigest))
                        Files.deleteIfExists(resourceUpdatePath);
                }
            }
            return sourceDigest;
        }

        public final boolean isFileValid() throws IOException {
            checkRemoved();

            if (Data.userConfig().getAutoUpdate())
                update();

            try {
//                System.out.printf("Expected: %s%nActual:   %s%n", new String(Base64.getEncoder().encode(expectedDigest)), new String(Base64.getEncoder().encode(fileDigest(!download))));
                return Arrays.equals(expectedDigest, fileDigest(!download));
            }
            catch (FileNotFoundException e) {
                return false;
            }
        }

        public final boolean updateAvailable(boolean forceCheck) throws IOException {
            checkRemoved();
            try {
                return !Arrays.equals(expectedDigest, sourceDigest(forceCheck));
            }
            catch (FileNotFoundException e) {
                return true;
            }
        }

        public final boolean update() throws IOException {
            checkRemoved();
            if (!updateAvailable(false))
                return false;

            if (download()) {
                Files.createDirectories(resourceDir);
                if (Files.exists(resourceUpdatePath)) {
                    Files.copy(resourceUpdatePath, resourcePath, StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(resourceUpdatePath);
                }
                else
                    Files.deleteIfExists(resourcePath);
            }
            expectedDigest = sourceDigest;
            fileDigest = sourceDigest;
            changed();
            return true;
        }

        public final URL sourceUrl() {
            checkRemoved();
            return sourceUrl;
        }

        public final boolean download() {
            checkRemoved();
            return download;
        }

        public void addChangeListener(ResourceEventListener listener) {
            checkRemoved();
            changeListeners.add(listener);
        }

        public boolean removeChangeListener(ResourceEventListener listener) {
            checkRemoved();
            return changeListeners.remove(listener);
        }

        protected void changed() {
            for (ResourceEventListener listener : changeListeners)
                listener.callback();
            saved.set(false);
        }

        /**
         * Adds an event listener that fires immediately before the element is removed
         *
         * @param listener The listener to call before removal
         */
        public void addRemovalListener(ResourceEventListener listener) {
            checkRemoved();
            removalListeners.add(listener);
        }

        public boolean removeRemovalListener(ResourceEventListener listener) {
            checkRemoved();
            return changeListeners.remove(listener);
        }

        public URL getResourceUrl() throws ResourceFileInvalidException, IOException {
            checkRemoved();
            if (!isFileValid())
                throw new ResourceFileInvalidException(name);
            else if (download())
                return resourcePath.toUri().toURL();
            else
                return sourceUrl;
        }

        void remove() throws IOException {
            for (ResourceEventListener listener : removalListeners)
                listener.callback();
            removed.setRelease(true);
            try {
                if (download) // Using the `download()` getter WILL BREAK
                    Files.delete(resourcePath);
            }
            finally {
                Files.deleteIfExists(resourceUpdatePath);
            }
        }

        protected void checkRemoved() {
            if (removed.getAcquire())
                throw new IllegalStateException("Cannot use removed book");
        }

        void save() {
            if (!saved.compareAndExchange(false, true)) {
                document.getDocumentElement().replaceChild(
                    getElement((T) this), // If this cast is incorrect, then someone really, REALLY fucked up, and everything is going to break anyway.,
                    XMLUtils.streamNodeListElements(document.getElementsByTagName("id"))
                        .filter(e -> e.getTextContent().equals(id.toString()))
                        .map(Element::getParentNode)
                        .findAny()
                        .orElseThrow()
                );
            }
        }
    }

    public record BaseResourceData(String name, UUID id, Digest digest, URL sourceUrl, boolean download) { }
}
