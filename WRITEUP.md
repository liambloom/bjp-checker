# Checker Writeup

As I am working on this project as part of a class (Computer Science Independent Study), I need to create a writup of the project.

Disclaimer: I am currently in the middle of reorganizing everything, so some links may be broken, and some names may be subject to change.

## Introduction &mdash; What is the Checker?

This program, checker, is a program that is able to check java programs using a predefined set of inputs and expected outputs. This predefined set is an xml file referred to in the program as a `Book`. For example, all of the Exercises and Programming Projects in <i>Building Java Programs 3rd Edition</i> are in [this book](bjp3-checker-annotations/src/main/resources/book.xml).

### Book Format

This book is separated into sections (in BJP3, these would be chapters), and then further into checked items (in BJP3, the checked items would be exercises and programming projects). Each checked item has one or more method, constructor, or "program" (which can be either a no-argument method *or* a main method), and each of these contains one or more "tests". The test can specify two preconditions: arguments (which can have checked postconditions), and the contents of `System.in` (blank by default). It can also specify postconditions: should the method throw an exception (and if so, of what type), what does it return (tests can be run on the return value as well, with the same format), and what does it print.

In addition, each book will be associated with annotations which can be used to mark anything relevant to a particular section or checked item. For example, BJP3 would have the [`@Chapter`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/Chapter.java), [`@Exercise`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/Exercise.java), and [`@ProgrammingProject`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/ProgrammingProject.java) annotations. Note that these annotations will probably be in a separate repository from the checker[^1].

## How does it work?

### Preparing the data

The method `Checker.check` is called with one argument, of type `CheckArgs`. I chose to do this because having a single method with too many arguments is confusing, so I chose to store all the arguments in a record (`CheckArgs`). `CheckArgs` has 5 fields, you can see their meaning in their javadoc. Most of these things will be specified fairly easily from either the CLI or the GUI. The only thing that will be a little trick will the the `paths` field, which contains paths to the classes that will be checked. It will be specified by the user as follows:

- In the CLI, the paths to be loaded are specified by one or more [globs](https://en.wikipedia.org/wiki/Glob_(programming)), separated by spaces or by [`System.pathSeparator`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html#pathSeparator).
- My current plan can be found at [#26](https://github.com/liambloom/checker/issues/26)[^1].

The classes at these paths will be loaded by a [`PathClassLoader`](internal/src/main/java/dev/liambloom/checker/internal/PathClassLoader.java). Once loaded, any classes that are not relevant to the section (chapter) being checked will be filtered out. The checker will know what section a particular class is relevant to by looking at the annotations. Any classes missing an annotation will be assumed not to be a part of any section, and will also be filtered out. If the `chapter` field of `CheckArgs` is empty, then it attempts to autodetect which section to check. 

- If all annotated classes are part fo the same section, then that section is checked.
- If there are annotated classes belonging to multiple sections, an exception is thrown.

After the classes are filtered, all the classes, constructors, methods, and fields are sorted into `Targets`s. There is one `Targets` object for each checked item. `Targets` is a set (it implements the set interface), and has four getter methods for the subsets of classes, constructors, methods, and fields.

Then, the tests for this section (chapter) will be read from the book in `CheckArgs`. Then, for each checkable item, `Test.mutliTest` is called with its respective `Targets` and tests (in XML form) to construct the test(s).

### Generating the tests

`Test.multiTest` goes through each XML method/constructor in the XML, and finds the corresponding method/constructor in its `Targets`[^2], and calls `Test.streamFromStaticExecutable` for each one.

`Test.streamFromStaticExecutable` verifies that the method/constructor has is accessible and has the correct parameters based on the XML (note: this verification should probably be moved to `Test.multiTest`). It then goes through the tests for the method/constructor, and calls `staticExectuableTest` for each one.

`Test.staticExecutableTest` reads all the pre- and post-conditions and constructs a `Test` to run them.


[^1]: Not yet implemented
[^2]: Partially implemented
