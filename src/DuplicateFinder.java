import java.io.*;
import java.util.*;

public class DuplicateFinder {
    private static void copyDirectory(final File sourceDirectory, final File destinationDirectory) throws IOException {
        assert destinationDirectory.exists() || destinationDirectory.mkdir();
        for (final String f : Objects.requireNonNull(sourceDirectory.list())) {
            copyJavaClassFile(new File(sourceDirectory, f), new File(destinationDirectory, f));
        }
    }

    public static void copyJavaClassFile(final File source, final File destination) throws IOException {
        if (source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source, destination);
        }
    }

    private static void copyFile(final File sourceFile, final File destinationFile) throws IOException {
        try (final InputStream in = new FileInputStream(sourceFile);
             final OutputStream out = new FileOutputStream(destinationFile)) {
            final byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }

    private static boolean deleteDirectory(final File directoryToBeDeleted) {
        final File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (final File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static int countUnique(final File dir) {
        System.out.println("Collecting all images..");
        final Set<String> images = new HashSet<>();
        for (final File directory : Objects.requireNonNull(dir.listFiles())) {
            if (directory.getName().endsWith(")")) {
                for (final File file : Objects.requireNonNull(directory.listFiles())) {
                    images.add(file.getName());
                }
            }
        }
        System.out.println("Found " + images.size() + " unique images");
        return images.size();
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Usage: DuplicateFinder <directory path>");
            return;
        }

        final File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.err.println(dir.getPath() + " is not directory");
            return;
        }

        final int before = countUnique(dir);
        assert dir.listFiles() != null;
        final Map<Long, List<File>> authors = new HashMap<>();
        for (final File directory : Objects.requireNonNull(dir.listFiles())) {
            final String fileName = directory.getName();
            if (fileName.endsWith(")")) {
                final int startPos = fileName.lastIndexOf('(');
                final long userId = Long.parseLong(fileName.substring(startPos + 1, fileName.length() - 1));
                authors.putIfAbsent(userId, new ArrayList<>());
                authors.get(userId).add(directory);
            }
        }

        for (final Map.Entry<Long, List<File>> entry : authors.entrySet()) {
            System.out.println("Sorting user " + entry.getKey() + "...");
            entry.getValue().sort((f, g) -> Long.compare(g.lastModified(), f.lastModified()));
            final File latest = entry.getValue().get(0);
            System.out.println("Latest folder is: " + latest.getName());
            for (int i = 1; i < entry.getValue().size(); ++i) {
                try {
                    System.out.println(entry.getValue().get(i).getName() + "\t->\t" + latest.getName());
                    copyDirectory(entry.getValue().get(i), latest);
                } catch (final IOException e) {
                    System.err.println("Error while copying from " + entry.getValue().get(i) + " to " + latest);
                    e.printStackTrace();
                    return;
                }
                deleteDirectory(entry.getValue().get(i));
            }
        }

        final int after = countUnique(dir);
        if (before == after) {
            System.out.println("Succeed");
        } else {
            System.out.printf("Was: %d, now: %d :(%n%n", before, after);
        }
    }
}
