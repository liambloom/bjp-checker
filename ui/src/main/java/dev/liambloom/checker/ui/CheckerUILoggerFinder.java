package dev.liambloom.checker.ui;

//import com.google.auto.service.AutoService;
import com.google.auto.service.AutoService;
import dev.liambloom.checker.ui.cli.PrintStreamLogger;

// The reason this is in ui and not internal is because someone using the api (internal)
//  should be able to choose their own logger, whereas ui *is* someone using the api, and
//  so it chooses to use that logger.
@AutoService(System.LoggerFinder.class)
public class CheckerUILoggerFinder extends System.LoggerFinder {
    public static final boolean DEBUG = "1".equals(System.getenv("CHECKER_DEBUG"));

    @Override
    public System.Logger getLogger(String name, Module module) {
        return new PrintStreamLogger(name, DEBUG, System.err);
    }
}
