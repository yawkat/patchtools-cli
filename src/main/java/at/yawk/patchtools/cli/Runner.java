package at.yawk.patchtools.cli;

import com.google.common.collect.ImmutableList;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import uk.co.thinkofdeath.patchtools.Patcher;
import uk.co.thinkofdeath.patchtools.wrappers.ClassPathWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

/**
 * @author yawkat
 */
@Slf4j
public class Runner {
    @Setter private Path inputJar;
    private List<Path> dependencies = Collections.emptyList();
    @Setter private Path outputJar;
    private final List<ClassMatcher> matchers = new ArrayList<>(Arrays.asList(ClassMatcher.ACCEPT));

    private ClassSet classSet;

    public void setDependencies(List<Path> dependencies) {
        this.dependencies = ImmutableList.copyOf(dependencies);
    }

    public void setDependencies(Path... dependencies) {
        setDependencies(Arrays.asList(dependencies));
    }

    public void addMatcher(ClassMatcher matcher) {
        matchers.add(0, matcher);
    }

    public void initPass() throws IOException {
        buildClassPool();
    }

    private void buildClassPool() throws IOException {
        log.info("Loading classes");
        classSet = new ClassSet(new ClassPathWrapper(
                Stream.concat(dependencies.stream(), Stream.of(inputJar))
                        .map(Path::toFile).toArray(File[]::new)
        ));

        try (FileSystem zipFs = openZipFs(inputJar)) {
            for (Path root : zipFs.getRootDirectories()) {
                Files.walk(root)
                        .filter(path -> path.toString().toLowerCase().endsWith(".class"))
                        .filter(path -> {
                            String className = path.toString();
                            if (className.startsWith("/")) { className = className.substring(1); }
                            className = className.substring(0, className.length() - 6);
                            className = className.replace('/', '.');
                            for (ClassMatcher matcher : matchers) {
                                Optional<Boolean> include = matcher.include(className);
                                log.trace("Match {} @ {} = {}", className, matcher, include);
                                if (include.isPresent()) {
                                    return include.get();
                                }
                            }
                            // should not happen since matchers contains a catchall by default
                            throw new IllegalStateException("Unmatched class " + className);
                        })
                        .forEach(path -> {
                            try {
                                classSet.add(Files.newInputStream(path), true);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        }

        classSet.simplify();
    }

    public void applyPatch(Path patch) throws IOException {
        try (Reader reader = Files.newBufferedReader(patch)) {
            applyPatch(reader);
        }
    }

    public void readHeader(Path patch) throws IOException {
        try (Reader reader = Files.newBufferedReader(patch)) {
            // read header lines
            //noinspection StatementWithEmptyBody
            while (readHeaderLine(reader)) {}
        }
    }

    private boolean readHeaderLine(Reader reader) throws IOException {
        BufferedReader buffered = reader instanceof BufferedReader ?
                (BufferedReader) reader : new BufferedReader(reader);

        // peek 10 bytes (maximum of 2 comment chars + include / exclude)
        buffered.mark(10);
        char[] buf = new char[10];
        int len = buffered.read(buf);
        buffered.reset();

        if (len < 10) {
            return false;
        }
        int prefixChars;
        String command = new String(buf);
        if (command.startsWith("//")) {
            command = command.substring(2);
            prefixChars = 2;
        } else if (command.startsWith("#")) { // # is currently not supported by patchtools but will probably be added
            command = command.substring(1);
            prefixChars = 1;
        } else if (command.trim().isEmpty()) {
            buffered.readLine();
            return true;
        } else {
            // actual commands follow, stop reading
            return false;
        }

        Consumer<String> commandFactory;
        switch (command.toLowerCase()) {
        case "include ":
            commandFactory = s -> addMatcher(new ClassMatcher.Include(s));
            prefixChars += 8;
            break;
        case "exclude ":
            commandFactory = s -> addMatcher(new ClassMatcher.Exclude(s));
            prefixChars += 8;
            break;
        default:
            buffered.readLine();
            return true;
        }
        buffered.skip(prefixChars);

        String args = buffered.readLine();
        commandFactory.accept(args);
        return true;
    }

    private void applyPatch(Reader reader) {
        log.info("Applying patch");
        Patcher patcher = new Patcher(classSet);
        patcher.apply(reader);
        log.info("Patches applied");
    }

    public void save() throws IOException {
        log.info("Saving patched jar");
        log.debug("Copying source jar to output");
        Files.copy(inputJar, outputJar, StandardCopyOption.REPLACE_EXISTING);
        try (FileSystem targetFs = openZipFs(outputJar)) {
            for (String className : ImmutableList.copyOf(classSet)) {
                log.debug("Storing class {}", className);
                String classFileName = className.replace('.', '/') + ".class";
                byte[] bytes = classSet.getClass(className);
                if (bytes == null) {
                    log.debug("Got null bytes for class {}, is it hidden?", className);
                    continue;
                }
                log.trace("{} bytes", bytes.length);
                Files.write(targetFs.getPath(classFileName), bytes, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
    }

    private static FileSystem openZipFs(Path path) throws IOException {
        return FileSystems.newFileSystem(path, null);
    }
}
