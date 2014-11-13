package at.yawk.patchtools.cli;

import at.yawk.logging.jul.FormatterBuilder;
import at.yawk.logging.jul.Loggers;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;

/**
 * @author yawkat
 */
public class Main {
    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();

        OptionSpec<Level> logLevel = parser.acceptsAll(Arrays.asList("l", "log-level"))
                .withRequiredArg()
                .withValuesConvertedBy(new ValueConverter<Level>() {
                    @Override
                    public Level convert(String value) {
                        return Level.parse(value);
                    }

                    @Override
                    public Class<? extends Level> valueType() {
                        return Level.class;
                    }

                    @Override
                    public String valuePattern() {
                        return null;
                    }
                })
                .defaultsTo(Level.INFO);

        OptionSpec<Path> inputFile = parser.accepts("i", "input")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.getInstance());

        OptionSpec<Path> outputFile = parser.accepts("o", "output")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.getInstance());

        OptionSpec<Path> dependencies = parser.accepts("dependency")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.getInstance())
                .defaultsTo(new Path[0]);

        OptionSpec<Path> patches = parser.nonOptions()
                .withValuesConvertedBy(PathValueConverter.getInstance());

        OptionSet options = parser.parse(args);

        Logger logger = Logger.getLogger("at.yawk.patchtools.cli");
        logger.setLevel(logLevel.value(options));

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(logLevel.value(options));

        Loggers.replaceHandlers(logger, handler);
        handler.setFormatter(FormatterBuilder.createDefault().build());

        Runner runner = new Runner();
        runner.setDependencies(dependencies.values(options));
        runner.setInputJar(inputFile.value(options));
        runner.setOutputJar(outputFile.value(options));

        for (Path patch : patches.values(options)) {
            runner.readHeader(patch);
            runner.initPass();
            runner.applyPatch(patch);
        }
        runner.save();
    }
}
