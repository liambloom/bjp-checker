package dev.liambloom.checker.internal;

import dev.liambloom.checker.NotYetImplementedError;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.TestStatus;
import org.w3c.dom.Element;

public class Post {
    private final Element e;

    public Post(Element e) {
        this.e = e;
    }

    public Result<TestStatus> check(Object o) {
        throw new NotYetImplementedError("Check Post-condition");
    }

    // TODO: toString
}
