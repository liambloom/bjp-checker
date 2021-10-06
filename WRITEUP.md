# Checker Writeup

As I am working on this project as part of a class (Computer Science Independent Study), I need to create a writup of the project.

Disclaimer: I am currently in the middle of reorganizing everything, so some links may be broken, and some names may be subject to change.

## Introduction &mdash; What is the Checker?

This program, checker, is a program that is able to check java programs using a predefined set of inputs and expected outputs. This predefined set is an xml file referred to in the program as a `Book`. For example, all of the Exercises and Programming Projects in <i>Building Java Programs 3rd Edition</i> are in [this book](bjp3-checker-annotations/src/main/resources/book.xml).

### Book Format

This book is separated into sections (in BJP3, these would be chapters), and then further into checked items (in BJP3, the checked items would be exercises and programming projects). Each checked item has one or more method, constructor, or "program" (which can be either a no-argument method *or* a main method), and each of these contains one or more "tests". The test can specify two preconditions: arguments (which can have checked postconditions), and the contents of `System.in` (blank by default). It can also specify postconditions: should the method throw an exception (and if so, of what type), what does it return (tests can be run on the return value as well, with the same format), and what does it print.

In addition, each book will be associated with annotations which can be used to mark anything relevant to a particular section or checked item. For example, BJP3 would have the [`@Chapter`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/Chapter.java), [`@Exercise`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/Exercise.java), and [`@ProgrammingProject`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/ProgrammingProject.java) annotations. Note that these annotations will probably be in a separate repository from the checker. Also, I have not yet implemented this feature.

## How does it work?

The method `Checker.check` is called with one argument, of type `CheckArgs`. I chose to do this because having a single method with too many arguments is confusing, so I chose to store all the arguments in a record (`CheckArgs`). `CheckArgs` has 5 fields, you can see their meaning in their javadoc. Most of these things will be specified fairly easily from either the CLI or the GUI. The only thing that will be a little trick will the the `paths` field, which contains paths to the classes that will be checked. It will be specified by the user as follows:

- In the CLI, the paths to be loaded are specified by one or more [globs](https://en.wikipedia.org/wiki/Glob_(programming)), separated by spaces or by [`System.pathSeparator`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html#pathSeparator).
- The part of the GUI used to specify paths has not yet been implemented, but my current plan can be found at [#26](https://github.com/liambloom/checker/issues/26).

The classes at these paths will be loaded by a [`PathClassLoader`](internal/src/main/java/dev/liambloom/checker/internal/PathClassLoader.java). Once loaded, any classes that are not relevant to the section (chapter) being checked will be filtered out. The checker will know what section a particular class is relevant to by looking at the annotations. Any classes missing an annotation will be assumed not to be a part of any section, and will also be filtered out. If the `chapter` field of `CheckArgs` is empty, then it attempts to autodetect which section to check. 

- If all annotated classes are part fo the same section, then that section is checked.
- If there are annotated classes belonging to multiple sections, an exception is thrown.

After the classes are filtered, all the classes, constructors, methods, and fields are sorted into `Targets`s. There is one `Targets` object for each checked item.

### [`Checker`](internal/src/main/java/dev/liambloom/checker/internal/Checker.java)

The method `Checker.check` is called with one argument, of type `CheckArgs`. I chose to do this because having a single method with too many arguments is confusing, so I chose to store all the arguments in a record (`CheckArgs`). `CheckArgs` has 5 fields, you can see their meaning in their javadoc. The 

Once the specified classes are loaded, the [`Checker`](internal/src/main/java/dev/liambloom/checker/internal/Checker.java) filters them so that only the one for the section (chapter) that will be checked are included. Each class should have an annotation (for example, [`@Chapter`](bjp3-checker-annotations/src/main/java/dev/liambloom/checker/Chapter.java)[^1]) which marks what section is belongs to; any classes missing this annotation or marked as belonging to a section

[^1]: See [#41](https://github.com/liambloom/checker/issues/41)
