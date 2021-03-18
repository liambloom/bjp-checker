package io.github.liambloom.tests.book.bjp3;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

class Main {
    public static Main app;
    public final Arguments args;
    public final String here;
    public final Debugger debugger = new Debugger();

    public Main(String[] rawArgs) throws URISyntaxException {
        File f = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (f.isFile())
            here = f.getParent();
        else
            here = f.getPath();

        args = new Arguments(rawArgs);
    }

    public static void main(String[] rawArgs) {
        try {
            // -[x] Initialization
            app = new Main(rawArgs);

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
            if (false /* TODO: args.debug */ && e.getCause() != null)
                e.getCause().printStackTrace();
        }
        catch (Throwable e) {
            e.printStackTrace();
            app.debugger.internalError(e);
        }
    }


}
