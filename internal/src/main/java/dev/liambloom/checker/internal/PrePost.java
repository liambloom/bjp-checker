package dev.liambloom.checker.internal;

import dev.liambloom.checker.NotYetImplementedError;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.books.TestStatus;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PrePost {
    private final Element e;
    private final Object pre;
    private final Post post;

    public PrePost(Element e) {
        this.e = e;
        pre = parseJavaValue((Element) e.getFirstChild().getFirstChild());
        post = Optional.ofNullable(e.getChildNodes().item(2))
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .map(Post::new)
            .orElse(null);
    }

    public Object getPre() {
        return pre;
    }

    public Result<TestStatus> checkPost(Object o) {
        if (post == null)
            return new Result<>(null /* TODO */, TestStatus.OK);
        else
            return post.check(o);
    }





    private Object parseJavaItem(Element e) {
        if (e.getTagName().equals("class")) {
            throw new NotYetImplementedError("PrePost parse java <class> item");
        }
        else
            return parseJavaValue(e);
    }
}
