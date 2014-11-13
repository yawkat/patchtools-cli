package at.yawk.patchtools.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.ValueConverter;
import lombok.Getter;

/**
 * @author yawkat
 */
public class PathValueConverter implements ValueConverter<Path> {
    @Getter private static final ValueConverter<Path> instance = new PathValueConverter();

    private PathValueConverter() {}

    @Override
    public Path convert(String value) {
        return Paths.get(value);
    }

    @Override
    public Class<? extends Path> valueType() {
        return Path.class;
    }

    @Override
    public String valuePattern() {
        return null;
    }
}
