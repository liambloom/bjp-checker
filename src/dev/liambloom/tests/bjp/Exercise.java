package dev.liambloom.tests.bjp;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

// TODO: Is the @Repeatable? Can 1 method do multiple exercises at once (not changed)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ CONSTRUCTOR, FIELD, METHOD, TYPE })
@Repeatable(Exercises.class)
public @interface Exercise {
    int value();
}
