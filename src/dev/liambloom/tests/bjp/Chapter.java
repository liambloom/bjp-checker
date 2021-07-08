package dev.liambloom.tests.bjp;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Chapter {
    int value();
}
