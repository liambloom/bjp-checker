package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.Book;
import dev.liambloom.checker.books.Chapter;
import dev.liambloom.checker.books.Meta;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class XMLBook implements Book {
    static final XPathFactory xpf = XPathFactory.newInstance();
    private final XPath xpath = xpf.newXPath();
    private final Document document;
    private final Meta meta;
    private final boolean optimizeForSingleChapter;
    private boolean actualOptimizeForSingleChapter;
    private XMLBookChapter hasChapter = null;


    XMLBook(Document document, Meta meta, boolean optimizeForSingleChapter) {
        this.document = document;
        this.meta = meta;
        actualOptimizeForSingleChapter = this.optimizeForSingleChapter = optimizeForSingleChapter;
    }

    @Override
    public synchronized Chapter getChapter(int chapter) {
        if (actualOptimizeForSingleChapter) {
            if (hasChapter != null) {
                hasChapter.cloneElement();
                hasChapter = null;
                actualOptimizeForSingleChapter = false;
            }
            else
                return hasChapter = new XMLBookChapter(getChapterElement(chapter));
        }
        return new XMLBookChapter((Element) getChapterElement(chapter).cloneNode(true));
    }

    private Element getChapterElement(int chapter) {
        try {
            return (Element) xpath.evaluate("/book/chapter[@num='" + chapter + "']", document, XPathConstants.NODE);
        }
        catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Meta getMeta() {
        return meta;
    }

    public boolean isOptimizedForSingleChapter() {
        return optimizeForSingleChapter;
    }
}
