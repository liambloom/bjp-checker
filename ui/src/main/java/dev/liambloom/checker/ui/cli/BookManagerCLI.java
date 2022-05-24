package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.books.Result;
import dev.liambloom.checker.ui.Data;
import dev.liambloom.checker.ui.ResourceManager;
import dev.liambloom.util.function.FunctionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class BookManagerCLI extends ResourceManagerCLI<Data.BookManager, Data.BookManager.SelfLoadingBook> {
    public BookManagerCLI(Data.BookManager inner) {
        super(inner);
    }

    @Override
    public void evaluate(String[] args, int start) throws IOException {
        if (args.length == 0)
            throw new UserErrorException("Missing argument. See `chk " + inner.getPluralName() + " --help' for help.");
        switch (args[start++]) {
            case "add" -> {
                Main.assertArgsPresent(args, start, "name", "source", "parser");
                String parserName = args[start + 2];
                try {
                    inner.add(args[start], Main.resolveResource(args[start + 1]), Optional.ofNullable(Data.parsers().get(args[4])).orElseThrow(() -> new UserErrorException("Parser `" + parserName + "` doesn't exist")));
                }
                catch (IllegalArgumentException e) {
                    throw new UserErrorException(e.getMessage(), e);
                }
            }
            case "rename" -> {
                Main.assertArgsPresent(args, start, "old name", "new name");
                Data.BookManager.SelfLoadingBook book = Data.books().get(args[2]);
                if (book == null)
                    throw new UserErrorException("Book `" + args[2] + "` does not exist");
                try {
                    book.setName(args[3]);
                }
                catch (IllegalArgumentException e) {
                    throw new UserErrorException(e.getMessage(), e);
                }
            }
            case "validate" -> {
                if (args.length == 2)
                    throw new UserErrorException("Missing argument after `validate`");
                try {
                    new ResultPrinter().printResults((args.length == 3 && (args[2].equals("-a") || args[2].equals("--all"))
                        ? Data.books().stream()
                        : Arrays.stream(args).skip(2).map(Data.books()::get))
                        .map(FunctionUtils.unchecked(Data.BookManager.SelfLoadingBook::validate))
                        .toArray(Result[]::new));
                }
                catch (NullPointerException e) {
                    throw new UserErrorException(e.getMessage(), e);
                }
            }
            default -> super.evaluate(args, start);
        }
    }

    @Override
    protected String[] getAdditionalListProperties(Data.BookManager.SelfLoadingBook book) {
        return new String[]{ book.getParser().map(Data.ParserManager.ParserRecord::getName).orElse("[removed]") };
    }
}
