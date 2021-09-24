package dev.liambloom.checker.bjp.api;

import org.w3c.dom.Element;

public class PrePost {
    private final Element e;

    public PrePost(Element e) {
        this.e = e;
    }

    public Object getPre() {
        return parseJavaItem((Element) e.getFirstChild().getFirstChild());
    }

    public void checkPost(Object o) {

    }

    private static Object parseJavaItem(Element e) {
        return switch (e.getTagName()) {
            case "class" -> null; // TODO
            case "byte" -> Byte.parseByte(e.getTextContent());
            case "short" -> Short.parseShort(e.getTextContent());
            case "int" -> Integer.parseInt(e.getTextContent());
            case "long" -> Long.parseLong(e.getTextContent());
            case "float" -> Float.parseFloat(e.getTextContent());
            case "double" -> Double.parseDouble(e.getTextContent());
            case "char" -> e.getTextContent().charAt(0);
            case "boolean" -> Boolean.parseBoolean(e.getTextContent());
            case "String" -> e.getTextContent();
            case "File" -> null; // TODO
            default -> throw new IllegalArgumentException("Document was improperly validated");
        };
    }
}
