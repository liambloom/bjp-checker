package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.Book;
import dev.liambloom.checker.books.Chapter;
import dev.liambloom.checker.books.Checkable;
import dev.liambloom.checker.books.CheckableType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

public class XMLBookChapter implements Chapter {
    private final XPath xpath = XMLBook.getXPath();
    private final Book book;
    private Element element;

    XMLBookChapter(Element element, Book book) {
        this.element = element;
        this.book = book;
    }

    @Override
    public synchronized Checkable getCheckable(CheckableType<?> type, int number) {
        Element e;
        try {
            e = (Element) ((Node) xpath
                .evaluate(String.format("checkable[@type='%s'][@num='%d']", type.name(), number), element, XPathConstants.NODE))
                .cloneNode(true);
        }
        catch (XPathExpressionException ex) {
            throw new RuntimeException();
        }
        return new XMLBookCheckable(type.name() + " " + number, e, this);
    }

    @Override
    public Book getBook() {
        return book;
    }

    synchronized void cloneElement() {
        element = (Element) element.cloneNode(true);
    }
}
