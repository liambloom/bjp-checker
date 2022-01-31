package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.*;
import dev.liambloom.util.XMLUtils;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XMLBookCheckable implements Checkable {
    private static final Pattern TRAILING_SPACES_AND_NEWLINE = Pattern.compile("\\s*\\R");
    private final String name;
    private final Element element;

    XMLBookCheckable(String name, Element element) {
        this.name = name;
        this.element = element;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public synchronized Test[] tests(StaticExecutableTest.Factory factory) {
        return XMLUtils.streamNodeListElements(element.getChildNodes())
            .map(element -> {
                StaticExecutableTest.Type type = StaticExecutableTest.Type.valueOf(element.getTagName().toUpperCase(Locale.ENGLISH));
                int i = 0;
                List<Element> children = XMLUtils.streamNodeListElements(element.getChildNodes()).toList();
                InputStream in = children.get(i).getTagName().equals("System.in")
                    ? new ByteArrayInputStream(children.get(i++).getTextContent().getBytes())
                    : null;
                if (children.get(i).getTagName().equals("this")) // TODO: Update Schema
                    throw new IllegalArgumentException("Element <this> invalid in top level method");
                List<Element> argElements = children.get(i).getTagName().equals("arguments")
                    ? XMLUtils.streamNodeListElements(children.get(i++).getChildNodes()).toList()
                    : Collections.emptyList();
                Iterator<Element> iter = argElements.iterator();
                Object[] args = new Object[argElements.size()];
                Post[] post = new Post[argElements.size()];
                for (int j = 0; iter.hasNext(); j++) {
                    Element e = iter.next();
                    List<Element> prePost = XMLUtils.streamNodeListElements(e.getChildNodes()).toList();
                    args[j] = parseJavaItem(prePost.get(0));
                    // TODO: set post
                }
                Post expectedReturns;
                String expectedOut;
                Class<? extends Throwable> expectedThrows;
                Map<Path, String> writesTo;
                if (children.get(i).getTagName().equals("throws")) {
                    try {
                        //noinspection unchecked,UnusedAssignment
                        expectedThrows = (Class<? extends Throwable>) ClassLoader.getSystemClassLoader().loadClass(children.get(i++).getTextContent());
                    }
                    catch (ClassNotFoundException | ClassCastException e) {
                        throw new IllegalStateException("This should not have passed validation.", e);
                    }
                    expectedReturns = null;
                    expectedOut = null;
                    writesTo = Collections.emptyMap();
                }
                else {
                    expectedThrows = null;
                    expectedReturns = Optional.ofNullable(children.get(i))
                        .filter(n -> n.getTagName().equals("returns"))
                        .map((Function<? super Element, Post>) e -> {
                            throw new Error("Post condition creation not yet implemented");
                        })
                        .orElse(null);
                    if (expectedReturns != null)
                        i++;
                    String rawExpectedPrints = Optional.ofNullable(children.get(i))
                        .filter(n -> n.getTagName().equals("prints"))
                        .map(Element::getTextContent)
                        .map(String::stripIndent)
                        .orElse(null);
                    if (rawExpectedPrints == null)
                        expectedOut = null;
                    else {
                        expectedOut = cleansePrint(rawExpectedPrints);
                        //noinspection UnusedAssignment
                        i++;
                    }
                    writesTo = children.stream()
                        .collect(Collectors.toMap(
                            e -> Path.of(e.getAttribute("href")),
                            e -> cleansePrint(e.getTextContent())
                        ));
                }
                return factory.newInstance(type, new StaticExecutableTest.Conditions(in, args, post, expectedOut, expectedReturns, expectedThrows, writesTo));
            })
            .toArray(Test[]::new);
    }

    private Object parseJavaValue(Element e) {
        return switch (e.getTagName()) {
            case "byte" -> Byte.parseByte(e.getTextContent());
            case "short" -> Short.parseShort(e.getTextContent());
            case "int" -> Integer.parseInt(e.getTextContent());
            case "long" -> Long.parseLong(e.getTextContent());
            case "float" -> Float.parseFloat(e.getTextContent());
            case "double" -> Double.parseDouble(e.getTextContent());
            case "char" -> e.getTextContent().charAt(0);
            case "boolean" -> Boolean.parseBoolean(e.getTextContent());
            case "String" -> e.getTextContent();
            case "File" -> new File(e.getTextContent());
            case "Path" -> Path.of(e.getTextContent());
            case "Scanner" -> new Scanner(new ByteArrayInputStream(e.getTextContent().getBytes()));
            case "Array" -> parseJavaListElement(e, ArrayList::new).toArray();
            case "ArrayList" -> parseJavaListElement(e, ArrayList::new);
            case "LinkedList" -> parseJavaListElement(e, LinkedList::new);
            case "Stack" -> parseJavaListElement(e, Stack::new);
            case "HashSet" -> parseJavaListElement(e, HashSet::new);
            case "TreeSet" -> parseJavaListElement(e, TreeSet::new);
            // TODO
            default -> throw new IllegalArgumentException("Document was improperly validated");
        };
    }

    private <T, C extends Collection<T>> C parseJavaListElement(Element e, Supplier<C> supplierFactory) {
        try {
            return XMLUtils.streamNodeListElements(e.getChildNodes())
                .map(this::parseJavaValue)
                .map(getClass(e.getAttribute("elementType"))::cast)
                .map(i -> (T) i)
                .collect(Collectors.toCollection(supplierFactory));
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Document references nonexistent class");
        }
    }

    private Object parseJavaItem(Element e) {
        if (e.getTagName().equals("class")) {
            throw new Error("Not yet implemented: PrePost parse java <class> item");
        }
        else
            return parseJavaValue(e);
    }

    public static Class<?> getClass(String name) throws ClassNotFoundException {
        return getClass(ClassLoader.getSystemClassLoader(), name);
    }

    public static Class<?> getClass(ClassLoader loader, String name) throws ClassNotFoundException {
        int arrayDepth = 0;
        StringBuilder componentName = new StringBuilder(name);
        componentName.trimToSize();
        while (componentName.length() > 2 && componentName.charAt(componentName.length() - 2) == '[' && componentName.charAt(componentName.length() - 1) == ']') {
            arrayDepth++;
            componentName.delete(componentName.length() - 2, componentName.length());
        }
        Class<?> clazz = switch (name) {
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "boolean" -> boolean.class;
            case "char" -> char.class;
            default -> loader.loadClass(name);
        };
        for (int i = 0; i < arrayDepth; i++)
            clazz = clazz.arrayType();
        return clazz;
    }

    private static String cleansePrint(String raw) {
        String[] lines = TRAILING_SPACES_AND_NEWLINE.split(raw);
        if (lines.length == 0)
            return "";
        Stream<String> linesStream = Arrays.stream(lines);
        if (lines[lines.length - 1].isBlank())
            linesStream = linesStream.limit(lines.length - 1);
        if (lines[0].isEmpty())
            linesStream = linesStream.skip(1);
        return linesStream
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
