package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.Chapter;
import dev.liambloom.checker.books.Checkable;
import dev.liambloom.checker.books.CheckableType;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

public class XMLBookChapter implements Chapter {
    private XPath xpath = XMLBook.xpf.newXPath();
    private Element element;


    XMLBookChapter(Element element) {
        this.element = element;
    }

    @Override
    public synchronized Checkable getCheckable(CheckableType<?> type, int number) {
        Element e = xpath.evaluate(String.format("checkable[@type='%s'][@num='%d']", type.name(), number), element, XPathConstants.NODE);
    }

    synchronized void cloneElement() {
        element = (Element) element.cloneNode(true);
    }
}
