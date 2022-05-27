package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.*;
import dev.liambloom.util.XMLUtils;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.*;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class XMLBookReader {
    private static final XPathFactory xpf = XPathFactory.newInstance();
    private static final DocumentBuilderFactory dbf;

    static {
        dbf = DocumentBuilderFactory.newInstance();
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        try {
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
            dbf.setSchema(factory.newSchema(
                new StreamSource(XMLBookReader.class.getResourceAsStream("/book-schema.xsd"))));
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        dbf.setNamespaceAware(true);
    }

    // Set at initialization
    private final System.Logger logger = System.getLogger(XMLBookReader.class.getName() + System.identityHashCode(this));
    private final XPath xpath = xpf.newXPath();
    private final BookLocator locator;
    private final String name;
//    private MessageDigest digest;

    // Set in parseAndValidateDocument()
    private Document document = null;
    private URLClassLoader classLoader;
    private Result<BookValidationStatus> result = null;
    private Exception documentParseException;
    private CheckableType<? extends Annotation> chapterType = null;
    private CheckableType<? extends Annotation>[] checkableTypes = null;

    // Set in getResources()
    private URI[] resources;

    public XMLBookReader(String name, BookLocator locator) {
        this.name = name;
        this.locator = locator;
    }

    public Result<BookValidationStatus> validateBook() throws IOException {
        try {
            parseAndValidateDocument();
        }
        catch (IOException | Error | RuntimeException e) {
            if (e != documentParseException)
                throw e;
        }
        catch (Exception e) {
            if (e != documentParseException)
                throw new IllegalStateException("Checked exception thrown during document parsing, but result is null", e);
        }
        assert result != null;
        return result;
    }

    public Document getDocument() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return document;
    }

    private void parseAndValidateDocument() throws IOException, SAXException, ClassNotFoundException, NoSuchMethodException {
//        logger.log(System.Logger.Level.TRACE, "Parsing & validating document");
        /*if (document != null)
            logger.log(System.Logger.Level.TRACE, "Document already parsed");
        else */
        if (document != null)
            return;
        if (documentParseException == null) {
            ValidationErrorHandler handler = new ValidationErrorHandler(name);
            try {
//                logger.log(System.Logger.Level.TRACE, "Parsing document");
                DocumentBuilder db;
                try {
                    synchronized (dbf) {
                        db = dbf.newDocumentBuilder();
                    }
                }
                catch (ParserConfigurationException e) {
//                    logger.log(System.Logger.Level.DEBUG, "Failure to create document builder", e);
                    throw new RuntimeException(e);
                }
//                logger.log(System.Logger.Level.TRACE, "Document builder obtained");
                db.setErrorHandler(handler);
                try {
                    document = db.parse(/*new DigestInputStream(*/locator.url().openStream()/*, digest)*/);
//                    logger.log(System.Logger.Level.TRACE, "Successfully parsed document");
                }
                catch (SAXException e) {
//                    logger.log(System.Logger.Level.TRACE, "Error parsing document");
                    result = new Result<>(name, BookValidationStatus.INVALID, handler.getLogs());
                    throw e;
                }
                catch (NoSuchFileException e) {
//                    logger.log(System.Logger.Level.TRACE, "Book does not exist");
                    result = new Result<>(name, BookValidationStatus.NOT_FOUND);
                    throw new IllegalStateException("Book " + locator + " does not exist");
                }
                catch (Throwable e) {
//                    logger.log(System.Logger.Level.TRACE, "Non-sax error parsing document", e);
                    e.printStackTrace();
                    throw e;
                }
                // I'd like to wrap exceptions in SAXParseException, but I have no way to get the line/column number
                //  without using a SAX parser. Since I want a document to end with, I would need to either:
                //  a: parse it twice, or b: build my own DocumentBuilder.
                try {
                    URI bookUri = locator.url().toURI();
                    classLoader = new URLClassLoader(XMLUtils.streamNodeListElements((NodeList) xpath
                            .evaluate("/book/meta/classPath/include", document, XPathConstants.NODESET))
                        .map(Element::getTextContent)
                        .map(bookUri::resolve)
                        .map(FunctionUtils.unchecked(URI::toURL))
                        .toArray(URL[]::new));
                }
                catch (XPathExpressionException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                List<Element> checkableTypeElements = XMLUtils.streamNodeListElements(document.getElementsByTagName("checkableType")).toList();
                checkableTypes = new CheckableType[checkableTypeElements.size()];
                Stream.Builder<Exception> checkableTypeErrors = Stream.builder();
                for (int i = 0; i < checkableTypeElements.size(); i++) {
                    try {
                        checkableTypes[i] = parseCheckableType(checkableTypeElements.get(i));
                    }
                    catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | IllegalAccessException e) {
                        checkableTypeErrors.add(e);
                    }
                }

                Stream<Exception> typeErrors = Stream.of(
                        Stream.of(
                                elementOfType("parameter")
                                    .map(Node::getTextContent),
                                Stream.of(
                                    elementOfType("Array", "ArrayList", "LinkedList", "TargetArrayList", "Stack", "HashSet", "TreeSet", "TargetTree")
                                        .map(e -> e.getAttributeNode("elementType")),
                                    elementOfType("HashMap", "TreeMap")
                                            .flatMap(e -> Stream.of("keyType", "valueType")
                                                .map(e::getAttributeNode)),
                                    elementOfType("method", "program")
                                        .map(e -> e.getAttributeNode("in"))
                                )
                                    .flatMap(Function.identity())
                                    .filter(Objects::nonNull)
                                    .map(Attr::getValue)
                            )
                            .flatMap(Function.identity())
                            .map(String::trim)
                            .map(type -> switch (type) {
                                case "byte", "short", "int", "long", "float", "double", "boolean", "char", "this" -> null;
                                default -> {
                                    try {
                                        classLoader.loadClass(type);
                                        yield null;
                                    }
                                    catch (ClassNotFoundException e) {
                                        yield e;
                                    }
                                }
                            })
                            .filter(Objects::nonNull),
                        elementOfType("throws")
                            .map(Node::getTextContent)
                            .map(String::trim)
                            .map(type -> {
                                try {
                                    Throwable.class.isAssignableFrom(classLoader.loadClass(type));
                                    return null;
                                }
                                catch (ClassNotFoundException | ClassCastException e) {
                                    return e;
                                }
                            }),
                        elementOfType("chapterType")
                            .map(element -> {
                                try {
                                    chapterType = parseCheckableType(element);
                                    return null;
                                }
                                catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | IllegalAccessException e) {
                                    return e;
                                }
                            }),
                        checkableTypeErrors.build()
                    )
                    .flatMap(Function.<Stream<? extends Exception>>identity())
                    .filter(Objects::nonNull);

//                typeErrors = typeErrors

                List<Exception> typeErrorsList = typeErrors.toList();
                for (Exception e : typeErrorsList)
                    handler.log(System.Logger.Level.ERROR, e);
                if (!typeErrorsList.isEmpty())
//                    logger.log(System.Logger.Level.TRACE, "No type errors present");
                /*else*/ {
//                    logger.log(System.Logger.Level.TRACE, "Type errors present");
                    Exception err = typeErrorsList.get(0);
                    if (err instanceof ClassNotFoundException e)
                        throw e;
                    else if (err instanceof ClassCastException e)
                        throw e;
                    else if (err instanceof NoSuchMethodException e)
                        throw e;
                    else
                        throw new IllegalStateException("Type error is not a type error", err);
                }

//                if (!locator.supportsResourceLoading()
//                    && (document.getElementsByTagName("File").getLength() > 0 || document.getElementsByTagName("Path").getLength() > 0)) {
//                    UnsupportedOperationException e = new UnsupportedOperationException("Book document contains <File> or <Path> element, but Book does not support file resolution");
//                    handler.log(System.Logger.Level.ERROR, e);
//                    throw e;
//                }
            }
            catch (IOException | SAXException | ClassNotFoundException | RuntimeException | NoSuchMethodException e) {
                if (result == null){
                    handler.log(System.Logger.Level.ERROR, e);
                    result = new Result<>(name, BookValidationStatus.INVALID, handler.getLogs());
                }
                documentParseException = e;
                throw e;
            }
            finally {
                if (result == null) {
                    result = switch (handler.getMaxErrorKind()) {
                        case ALL -> new Result<>(name, BookValidationStatus.VALID);
                        case WARNING -> new Result<>(name, BookValidationStatus.VALID_WITH_WARNINGS, handler.getLogs());
                        default -> new Result<>(name, BookValidationStatus.INVALID, handler.getLogs());
                    };
                }
            }
        }
        else {
//            logger.log(System.Logger.Level.TRACE, "A previous attempt to parse the document failed");

            if (documentParseException instanceof SAXException e)
                throw e;
            else {
                try {
                    //noinspection ThrowableNotThrown,ResultOfMethodCallIgnored
                    BookParser.reThrowError(documentParseException);
                }
                catch (BookParserException | URISyntaxException ignored) {
                }
                throw new IllegalStateException("DocumentParseException had unexpected type " + documentParseException.getClass().getSimpleName());
            }
        }
    }

    public CheckableType<? extends Annotation> getChapterType() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return chapterType;
    }

    public CheckableType<? extends Annotation>[] getCheckableTypes() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return checkableTypes;
    }

    public URLClassLoader getClassLoader() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return classLoader;
    }

    @SuppressWarnings("RedundantThrows")
    public URI[] getResources() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException, URISyntaxException {
        if (resources != null)
            return resources;

        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/book/meta/rsc", getDocument(), XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

        return resources = XMLUtils.streamNodeListElements(nodes)
            .map(e -> e.getAttribute("href"))
            .map(FunctionUtils.unchecked((FunctionThrowsException<String, URI>) URI::new))
            .toArray(URI[]::new);
//                .map(locator.getURI()::resolve)
//                .map(FunctionUtils.unchecked(URI::toURL))
//                .toArray(URL[]::new);
    }

    private Stream<Element> elementOfType(String... types) {
        return Arrays.stream(types)
            .map(document::getElementsByTagName)
            .flatMap(XMLUtils::streamNodeListElements);
    }

    private CheckableType<?> parseCheckableType(Element e) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        Class<?> clazz = classLoader.loadClass(e.getAttribute("annotation"));
        if (!Annotation.class.isAssignableFrom(clazz))
            throw new ClassCastException("Cannot convert " + clazz.getName() + " to java.lang.Annotation");
        return new CheckableType<>(e.getAttribute("name"), (Class<? extends Annotation>) clazz);
    }
}
