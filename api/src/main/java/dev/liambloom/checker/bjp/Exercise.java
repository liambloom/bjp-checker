package dev.liambloom.checker.bjp;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ CONSTRUCTOR, FIELD, METHOD, TYPE })
@Repeatable(Exercises.class)
public @interface Exercise {
    int value();
}
