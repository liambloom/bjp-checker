package dev.liambloom.checker.internal;

import dev.liambloom.util.ResourcePool;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Util {
    private static final ResourcePool<XPath> xPathPool = new ResourcePool<>(XPathFactory.newInstance()::newXPath);

    private Util() {
    }

    public static final Pattern TRAILING_SPACES_AND_NEWLINE = Pattern.compile("\\s*\\R");

    public static String generateLoggerName() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getName()
            + "#" + ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }

    /*public static String normalizeLineSeparators(String s) {
        return LINE_SEPARATOR.matcher(s).replaceAll(System.lineSeparator());
    }*/

    public static String cleansePrint(String raw) {
        String[] lines = Util.TRAILING_SPACES_AND_NEWLINE.split(raw);
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



    public static Stream<Node> streamNodeList(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
            .parallel()
            .mapToObj(nodeList::item);
    }





    public static ResourcePool<XPath> getXPathPool() {
        return xPathPool;
    }
}
