package dev.liambloom.checker.internal;

import dev.liambloom.checker.shared.Util;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PrePost {
    private final Element e;
    private final UnaryOperatorThrowsIOException<Path> resolver;

    public PrePost(Element e, UnaryOperatorThrowsIOException<Path> resolver) {
        this.e = e;
        this.resolver = resolver;
    }

    public Object getPre() {
        return parseJavaItem((Element) e.getFirstChild().getFirstChild());
    }

    public void checkPost(Object o) {

    }

    private Stream<Object> parseJavaListElements(Element e) {
        return Util.streamNodeList(e.getChildNodes())
            .map(Element.class::cast)
            .map(this::parseJavaValue);
    }

    private Object parseJavaValue(Element e) {
        try {
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
                case "File" -> resolver.apply(Path.of(e.getTextContent())).toFile();
                case "Path" -> resolver.apply(Path.of(e.getTextContent()));
                case "Scanner" -> new Scanner(new ByteArrayInputStream(e.getTextContent().getBytes()));
                case "Array" -> parseJavaListElements(e)
                    .toArray(l -> {
                        try {
                            return (Object[]) Array.newInstance(Util.loadClass(e.getAttribute("elementType")), l);
                        }
                        catch (ClassNotFoundException ex) {
                            throw new IllegalArgumentException("Incorrectly validated document", ex);
                        }
                    });
                case "ArrayList" -> parseJavaListElements(e)
                    .map(Util.loadClass(e.getAttribute("elementType"))::cast)
                    .collect(Collectors.toCollection(ArrayList::new));
                case "LinkedList" -> parseJavaListElements(e)
                    .map(Util.loadClass(e.getAttribute("elementType"))::cast)
                    .collect(Collectors.toCollection(LinkedList::new));
                case "Stack" -> parseJavaListElements(e)
                    .map(Util.loadClass(e.getAttribute("elementType"))::cast)
                    .collect(Collectors.toCollection(Stack::new));
                case "HashSet" -> parseJavaListElements(e)
                    .map(Util.loadClass(e.getAttribute("elementType"))::cast)
                    .collect(Collectors.toCollection(HashSet::new));
                case "TreeSet" -> parseJavaListElements(e)
                    .map(Util.loadClass(e.getAttribute("elementType"))::cast)
                    .collect(Collectors.toCollection(TreeSet::new));
                // TODO
                default -> throw new IllegalArgumentException("Document was improperly validated");
            };
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Incorrectly validated document", ex);
        }
    }

    private Object parseJavaItem(Element e) {
        if (e.getTagName().equals("class")) {
            // TODO
            return null;
        }
        else
            return parseJavaValue(e);
    }
}
