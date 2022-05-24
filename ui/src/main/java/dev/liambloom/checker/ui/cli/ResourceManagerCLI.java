package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.ui.Data;
import dev.liambloom.checker.ui.ResourceManager;
import dev.liambloom.util.StringUtils;

import java.io.IOException;
import java.util.Iterator;

public class ResourceManagerCLI<T extends ResourceManager<E>, E extends ResourceManager<E>.Resource> {
    public final T inner;

    public ResourceManagerCLI(T inner) {
        this.inner = inner;
    }

    /**
     * Evaluates CLI arguments to perform on this resource manager
     *
     * @param args The arguments that specify what to do
     * @param start The index of the first argument to evaluate
     */
    public void evaluate(String[] args, int start) throws IOException {
        if (args.length == 0)
            throw new UserErrorException("Missing argument. See `chk " + inner.getPluralName() + " --help' for help.");
        switch (args[start++]) {
            // case "update"
//            case "add" -> {
//
//            }
            case "remove" -> {
                Main.assertArgsPresent(args, start, "name");
                inner.remove(inner.get(args[start]));
            }
            case "move" -> {
                Main.assertArgsPresent(args, 2, "name", "new URL");
                E resource = inner.get(args[start]);
                if (resource == null)
                    throw new UserErrorException(StringUtils.convertCase(inner.getSingleName(), StringUtils.Case.SENTENCE) + " `" + args[start] + "` does not exist");
                resource.setSourceUrl(Main.resolveResource(args[start + 1]));
            }
            case "list" -> {
                Main.assertArgsPresent(args, 2);
                String[][] strs = new String[Data.books().size()][];
                int[] maxColumnWidth = null;
                int colCount = 0;
                Iterator<E> iter = inner.iterator();
                for (int i = 0; iter.hasNext(); i++) {
                    E resource = iter.next();
                    String[] additionalProperties = getAdditionalListProperties(resource);
                    String[] properties = new String[additionalProperties.length + 2];
                    System.arraycopy(additionalProperties, 0, properties, 2, additionalProperties.length);
                    if (i == 0) {
                        colCount = properties.length + 2;
                        maxColumnWidth = new int[colCount];
                    }
                    else if (properties.length != colCount - 2)
                        throw new IllegalStateException("The number of additional properties was not consistent across " + inner.getPluralName());
                    properties[0] = resource.getName();
                    properties[1] = resource.getSourceUrl().toString();

                    for (int j = 0; j < colCount; j++) {
                        if (properties[j].length() > maxColumnWidth[j])
                            maxColumnWidth[j] = properties[j].length();
                    }
//                    strs[i][2] = resource.getParser().map(Data.ParserManager.ParserRecord::getName).orElse("[removed]");
                }
                for (String[] resource : strs) {
                    for (int i = 0; i < colCount; i++)
                        System.out.printf("%-" + maxColumnWidth[i] + "s  ", resource[i]);
                    System.out.println();
                }
            }
        }
    }

    /**
     * Provides an array with the string representations of all properties
     * other than {@code name} and {@code sourceUrl} to be included in the
     * list
     *
     * @param element The element for which to find the properties
     * @return An array of string representations of all values to include
     */
    protected String[] getAdditionalListProperties(E element) {
        return new String[0];
    }
}
