package io.github.liambloom.tests.bjp3;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Chapter {
    int value();
}
