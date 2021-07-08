package dev.liambloom.tests.bjp.checker;

/*import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Stream;

public class Results {
    private Results() {}

    public static void save(File tests, int chapter, Stream<TestResult> results) throws IOException {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tests.toPath()));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 4; i++) {
            if (hash[i] < 16)
                builder.append('0');
            builder.append(Integer.toString(hash[i], 16));
        }
        File file = new File(".chk" + File.separator + builder);
        file.mkdirs();
        File hashFile = new File(file + File.separator + "hash");
        if (hashFile.createNewFile())
            Files.write(hashFile.toPath(), hash);
        else if (!Arrays.equals(Files.readAllBytes(hashFile.toPath()), hash))
            // Collision

    }

    public static Results loadFromFile() {
        // TODO
        return null;
    }
}*/
