package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.Result;
import dev.liambloom.util.StringUtils;
import dev.liambloom.util.function.ConsumerThrowsException;
import dev.liambloom.util.function.FunctionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ResultPrinter {
    private boolean detailTitlePrinted;

    public void printResults(Result<?>[] s) throws IOException {
        detailTitlePrinted = false;
        System.getLogger(Long.toString(System.identityHashCode(this))).log(System.Logger.Level.TRACE, "Printing Results: %s", Arrays.toString(s));
        for (Result<?> r : s)
            printResultSimple(r, 0);
//        System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.INFO, "Detailed result printing coming soon!");
        System.out.println();
        for (Result<?> r : s)
            printResultDetails(r.name(), r);
        System.getLogger(Long.toString(System.identityHashCode(this))).log(System.Logger.Level.TRACE, "Done Printing Results: %s", Arrays.toString(s));
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
                l.logTo(System.getLogger(Main.class.getName()));
                r.consoleOutput().ifPresent(c -> System.out.println());
            });
            r.consoleOutput().ifPresent(FunctionUtils.unchecked((ConsumerThrowsException<ByteArrayOutputStream>) c -> {
                c.writeTo(System.out);
                System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "Console output");
            }));
            System.out.println();
        }
        for (Result<?> sub : r.subResults())
            printResultDetails(fullName + "." + sub.name(), sub);
    }
}
