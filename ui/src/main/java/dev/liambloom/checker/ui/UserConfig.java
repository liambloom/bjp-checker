package dev.liambloom.checker.ui;

import dev.liambloom.util.StringUtils;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Optional;

public class UserConfig extends PersistentData {
    public enum Property {
        AUTO_UPDATE("false"),
        DEFAULT_BOOK(null);
        public final String defaultValue;

        Property(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    UserConfig() throws IOException, IncompatibleSettingsVersionException, TransformerException, SAXException {
        super("settings");
    }

    private Optional<Node> getNode(Property property) {
        return Optional.ofNullable(document.getDocumentElement().getElementsByTagName(StringUtils.convertCase(property.name(), StringUtils.Case.CAMEL)).item(0));
    }

    public String get(Property property) {
        return getNode(property).map(Node::getTextContent).orElse(property.defaultValue);
    }

    public void set(Property property, String value) {
        changed.setRelease(true);
        getNode(property)
            .orElseGet(() -> {
                Node e = document.createElement(StringUtils.convertCase(property.name(), StringUtils.Case.CAMEL));
                document.getDocumentElement().appendChild(e);
                return e;
            })
            .setTextContent(value);

    }

    public void unset(Property property) {
        getNode(property).ifPresent(node -> node.getParentNode().removeChild(node));
    }

    public boolean getAutoUpdate() {
        String str = get(Property.AUTO_UPDATE);
        return str.equalsIgnoreCase("true") || str.equals("1");
    }

    public void setAutoUpdate(boolean value) {
        set(Property.AUTO_UPDATE, value + "");
    }

    public Optional<String> getDefaultBook() {
        return Optional.ofNullable(get(Property.DEFAULT_BOOK));
    }

    public void setAutoUpdate(String value) {
        set(Property.DEFAULT_BOOK, value + "");
    }

    public static boolean propertyExists(String name) {
        try {
            getProperty(name);
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Property getProperty(String str) {
        return Property.valueOf(StringUtils.convertCase(str, StringUtils.Case.CONST));
    }
}
