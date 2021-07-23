package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.shared.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

public class Checked {
    public Type type;
    public int num;
    Test[] tests;

    public enum Type {
        EXERCISE,
        PROGRAMMING_PROJECT;

        @Override
        public String toString() {
            if (this == EXERCISE)
                return "Exercise";
            else
                return "Programming Project";
        }
    }

    public Checked(List<AnnotatedElement> targets, Element tests) {
        NodeList list = tests.getElementsByTagName("test");
        this.tests = new Test[list.getLength()];
        for (int i = 0; i < list.getLength(); i++)
            this.tests[i] = new Test(targets, list.item(i));
    }

   // public Stream<TestResult>
}
