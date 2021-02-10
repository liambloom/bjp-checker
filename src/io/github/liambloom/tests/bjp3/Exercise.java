package io.github.liambloom.tests.bjp3;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

@Target({CONSTRUCTOR, FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Exercise {
    int value();
}
