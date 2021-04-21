package dev.liambloom.tests.book.bjp;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ CONSTRUCTOR, FIELD, METHOD, TYPE })
public @interface ProgrammingProject {
    int value();
}
