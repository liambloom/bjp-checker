package io.github.liambloom.tests.book.bjp3;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

@Target({ CONSTRUCTOR, FIELD, METHOD, TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProgrammingProject {
    int value();
}
