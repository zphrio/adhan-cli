package io.github.zphrio.adhan.cli.config;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public final class ConfigStore {

    private static final List<String> REQUIRED_KEYS =
            List.of("latitude", "longitude", "method", "madhab");

    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    public static Path defaultFile() {
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path base = (xdg == null || xdg.isBlank())
                ? Path.of(System.getProperty("user.home"), ".config")
                : Path.of(xdg);
        return base.resolve("adhan").resolve("config");
    }

    public Optional<Config> load() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            throw new InvalidConfigException("Could not read config at " + file + ": " + e.getMessage());
        }
        for (String key : REQUIRED_KEYS) {
            String value = props.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new InvalidConfigException(key + " is missing from " + file + ". Run: adhan config");
            }
        }
        return Optional.of(new Config(
                parseDouble("latitude", props.getProperty("latitude")),
                parseDouble("longitude", props.getProperty("longitude")),
                parseEnum("method", props.getProperty("method"), CalculationMethod.class),
                parseEnum("madhab", props.getProperty("madhab"), Madhab.class),
                TimeFormat.fromKey(props.getProperty("timeformat", "12h").trim())));
    }

    public void save(Config config) {
        String content = "latitude=" + config.latitude() + "\n"
                + "longitude=" + config.longitude() + "\n"
                + "method=" + config.method().name() + "\n"
                + "madhab=" + config.madhab().name() + "\n"
                + "timeformat=" + config.timeFormat().key() + "\n";
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, content);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save config to " + file, e);
        }
    }

    private static double parseDouble(String key, String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(key + " must be a number (got: " + value + ")");
        }
    }

    private static <E extends Enum<E>> E parseEnum(String key, String value, Class<E> type) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigException(key + " must be one of: "
                    + Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "))
                    + " (got: " + value + ")");
        }
    }
}
