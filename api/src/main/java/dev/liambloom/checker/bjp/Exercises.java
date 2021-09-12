package dev.liambloom.checker.bjp;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ CONSTRUCTOR, FIELD, METHOD, TYPE })
public @interface Exercises {
    Exercise[] value();
}
