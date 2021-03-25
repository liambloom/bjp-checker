package io.github.liambloom.tests.book.bjp3;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @version 1.0.0-alpha-1
 */
class App {
    public static App app;
    public final Arguments args;
    public final String here;
    public static final Debugger debugger;

    static {
        try {
            debugger = new Debugger();
        }
        catch (Throwable e) {
            System.err.println("There was an error initializing the checker");
            System.exit(1);
            // This is unreachable, but java doesn't have a noreturn return type,
            // so I need to convince java that this won't return so that it will
            // accept that debugger is set.
            throw new IllegalStateException("Unreachable");
        }
    }

    public App(String[] rawArgs) throws URISyntaxException {
        File f = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (f.isFile())
            here = f.getParent();
        else
            here = f.getPath();

        args = new Arguments(rawArgs);
    }

    public static void main(String[] rawArgs) throws URISyntaxException {
        try {
            // -[x] Initialization
            app = new App(rawArgs);

            // -[ ] Load Tests
            TestLoader.load();

            // -[x] Load Classes
            // TODO: maybe make an argument to run tests in another directory
            //final Class<?>[] classes = DirectoryClassLoader.loadClassesHere();

            // -[ ] Search classes to find correct chapter(s) and exercise(s)
            // -[ ] Retrieve/decode previous results
            // -[ ] Maybe find some way to store diff and compare (so you don't test
            //      unchanged methods)
            //      Note: Tests would also need to be re-run is dependencies (including java
            //      version) changed
            // -[ ] Run tests in tests.xml
            // -[ ] Print/save/encode/submit results
            //      Results could be: correct, incorrect, previously working, missing
        }
        catch (UserErrorException e) {
            app.debugger.error(e.getMessage());
            if (app.debugger.debugMode && e.getCause() != null)
                e.getCause().printStackTrace();
        }
        catch (Throwable e) {
            e.printStackTrace();
            app.debugger.internalError(e);
        }
    }


}
