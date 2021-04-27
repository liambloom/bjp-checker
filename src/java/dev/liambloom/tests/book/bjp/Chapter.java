package dev.liambloom.tests.book.bjp;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Chapter {
    int value();
}
