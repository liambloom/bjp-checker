package dev.liambloom.checker.ui;

//import com.google.auto.service.AutoService;
import com.google.auto.service.AutoService;
import dev.liambloom.checker.ui.cli.PrintStreamLogger;

import java.util.HashMap;
import java.util.Map;

// The reason this is in ui and not internal is because someone using the api (internal)
//  should be able to choose their own logger, whereas ui *is* someone using the api, and
//  so it chooses to use that logger.
@AutoService(System.LoggerFinder.class)
public class CheckerUILoggerFinder extends System.LoggerFinder {
    private static final Map<System.Logger.Level, Boolean> debugConfig = new HashMap<>();

    static {
        resetDebugConfig();
    }

    @Override
    public System.Logger getLogger(String name, Module module) {
        return new PrintStreamLogger(name, debugConfig, System.err);
    }

    public static void setDebugConfigString(String s) {
        resetDebugConfig();
        boolean putValue;
        for (int i = 0; i <  s.length(); i++) {
            switch (s.charAt(i)) {
                case '+' -> putValue = true;
                case '-' -> putValue = false;
                default -> {
                    if (i == 0)
                        throw new IllegalArgumentException("Debug config string must begin with '+' or '-'");
                    continue;
                }
            }
            debugConfig.put(switch (s.charAt(i)) {
                case 'e' -> System.Logger.Level.ERROR;
                case 'w' -> System.Logger.Level.WARNING;
                case 'i' -> System.Logger.Level.INFO;
                case 'd' -> System.Logger.Level.DEBUG;
                case 't' -> System.Logger.Level.TRACE;
                default -> throw new IllegalArgumentException("Unknown debug level: '" + s.charAt(i) + '\'');
            }, putValue);
        }
    }

    public static void resetDebugConfig() {
        debugConfig.clear();
        debugConfig.put(System.Logger.Level.ERROR, true);
        debugConfig.put(System.Logger.Level.WARNING, true);
        debugConfig.put(System.Logger.Level.INFO, true);
    }
}
