package dev.liambloom.tests.book.bjp.checker.old;

//import dev.liambloom.tests.book.bjp.checker.old.cli.Arguments;

import org.xml.sax.SAXException;

import java.io.File;
import java.net.URISyntaxException;


// TODO (maybe) (in installer): Possibly add to BlueJ/lib/userlib
// TODO: I probably need to add a GUI
class App {
    public static final String VERSION = "v1.0.0-alpha-1";
    public final String here;
    public Logger logger;

    public App(Logger logger) throws URISyntaxException {
        this.logger = logger;
        File f = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (f.isFile())
            here = f.getParent();
        else
            here = f.getPath();
    }

    // TODO: most things should happen in here, a new app can be constructed by cli and gui mains
    // Arguments should be PASSED IN, not generated here
    // Also, App.here should be static and/or lazy

    /*public static void main(String[] args) {
        // TODO: Security Policy
        // https://stackoverflow.com/q/46991566/11326662
        try {
            // -[x] Initialization
            //app = new App();

            /*if (Arguments.preParser(args))
                return;*/
            // TODO: run tests

            // -[ ] Load Tests
            //TestLoader.load();

            // -[x] Load Classes
            // TODO: maybe make an argument to run tests in another directory
            //final Class<?>[] classes = DirectoryClassLoader.loadClassesHere();

            // -[ ] Search classes to find correct chapter(s) and exercise(s)
            // -[ ] Retrieve/decode previous results
            // -[ ] Maybe find some way to store diff and compare (so you don't test
            //          unchanged methods)
            //      Note: Tests would also need to be re-run if dependencies (including java
            //          version) changed
            //      Idea: Use SHA-1 hash to check if files have been changed (this is what git does)
            //          Actually, SHA-1 is broken (has collisions), use something more secure, like
            //          SHA-256
            // -[ ] Run tests in bjp-3.xml
            // -[ ] Print/save/encode/submit results
            //          Results could be: correct, incorrect, previously working, missing
        /*}
        catch (UserErrorException e) {
            App.logger.error(e.getMessage());
            if (App.logger.debugMode && e.getCause() != null)
                e.getCause().printStackTrace();
        }
        catch (Throwable e) {
            // e.printStackTrace();
            App.logger.internalError(e);
        }
    }*/

    public SAXException validateXML() {

    }

    public Results readResultsFromFile() {

    }

    public Results runTests() {

    }

    public Results submit() {

    }
}
