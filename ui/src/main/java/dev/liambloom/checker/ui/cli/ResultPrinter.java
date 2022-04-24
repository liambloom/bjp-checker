package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.books.Result;
import dev.liambloom.util.StringUtils;
import dev.liambloom.util.function.ConsumerThrowsException;
import dev.liambloom.util.function.FunctionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultPrinter {
    private boolean detailTitlePrinted;
    private final System.Logger logger = new PrintStreamLogger(Long.toString(System.identityHashCode(this)),
        Stream.of(System.Logger.Level.values()).collect(Collectors.toMap(Function.identity(), e -> true)), System.out);
    private final System.Logger debugLogger = System.getLogger(Long.toString(System.identityHashCode(this)) + "-debug");

    public void printResults(Result<?>[] s) throws IOException {
        detailTitlePrinted = false;
//        debugLogger.log(System.Logger.Level.TRACE, "Printing Results: %s", Arrays.toString(s));
        for (Result<?> r : s)
            printResultSimple(r, 0);
        System.out.println();
        for (Result<?> r : s)
            printResultDetails(r.name(), r);
//        debugLogger.log(System.Logger.Level.TRACE, "Done Printing Results: %s", Arrays.toString(s));
    }

    private void printResultSimple(Result<?> r, int level) {
        System.out.printf("%s%s ... \u001b[%sm%s\u001b[0m%n", "\t".repeat(level), r.name(), r.status().color().ansi(), StringUtils.convertCase(r.status().toString(), StringUtils.Case.SPACE));
        for (Result<?> sub : r.subResults())
            printResultSimple(sub, level + 1);
    }

    @SuppressWarnings("RedundantThrows")
    private void printResultDetails(String fullName, Result<?> r) throws IOException {
        if (r.consoleOutput().isPresent() || r.logs().isPresent()) {
            if (!detailTitlePrinted) {
                System.out.println("details:");
                System.out.println();
                detailTitlePrinted = true;
            }
            System.out.printf("---- %s ----%n", fullName);
            r.logs().ifPresent(l -> {
                debugLogger.log(System.Logger.Level.DEBUG, "Logs are present");
                l.logTo(logger);
//                r.consoleOutput().ifPresent(c -> System.out.println());
            });
            r.consoleOutput().ifPresent(FunctionUtils.unchecked((ConsumerThrowsException<ByteArrayOutputStream>) c -> {
                debugLogger.log(System.Logger.Level.DEBUG, "Console output is present");
                logger.log(System.Logger.Level.INFO, "Console output:");
                System.out.println("  | " + String.join(System.lineSeparator() + "  | ", c.toString().split("\\R")));
            }));
            System.out.println();
        }
        for (Result<?> sub : r.subResults())
            printResultDetails(fullName + "." + sub.name(), sub);
    }
}
