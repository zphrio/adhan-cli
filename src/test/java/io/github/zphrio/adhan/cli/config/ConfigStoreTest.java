package io.github.zphrio.adhan.cli.config;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {

    private static final Config SAMPLE = new Config(24.7136, 46.6753,
            CalculationMethod.UMM_AL_QURA, Madhab.HANAFI, TimeFormat.TWENTY_FOUR_HOUR);

    @TempDir
    Path tempDir;

    private Path configFile() {
        return tempDir.resolve("adhan").resolve("config");
    }

    @Test
    void loadReturnsEmptyWhenFileMissing() {
        assertEquals(Optional.empty(), new ConfigStore(configFile()).load());
    }

    @Test
    void saveThenLoadRoundTrips() {
        ConfigStore store = new ConfigStore(configFile());
        store.save(SAMPLE);
        assertEquals(SAMPLE, store.load().orElseThrow());
    }

    @Test
    void saveWritesPlainKeyValueLines() throws IOException {
        new ConfigStore(configFile()).save(SAMPLE);
        String expected = """
                latitude=24.7136
                longitude=46.6753
                method=UMM_AL_QURA
                madhab=HANAFI
                timeformat=24h
                """;
        assertEquals(expected, Files.readString(configFile()));
    }

    @Test
    void saveLeavesNoTempFile() throws IOException {
        new ConfigStore(configFile()).save(SAMPLE);
        try (var files = Files.list(configFile().getParent())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void missingRequiredKeyFailsNamingIt() throws IOException {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(), "longitude=46.6753\nmethod=UMM_AL_QURA\nmadhab=SHAFI\n");
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new ConfigStore(configFile()).load());
        assertTrue(ex.getMessage().contains("latitude"));
    }

    @Test
    void invalidMethodListsValidValues() throws IOException {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(),
                "latitude=24.7\nlongitude=46.7\nmethod=BOGUS\nmadhab=SHAFI\n");
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new ConfigStore(configFile()).load());
        assertTrue(ex.getMessage().contains("method"));
        assertTrue(ex.getMessage().contains("UMM_AL_QURA"));
        assertTrue(ex.getMessage().contains("MUSLIM_WORLD_LEAGUE"));
    }

    @Test
    void nonNumericLatitudeFails() throws IOException {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(),
                "latitude=abc\nlongitude=46.7\nmethod=UMM_AL_QURA\nmadhab=SHAFI\n");
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new ConfigStore(configFile()).load());
        assertTrue(ex.getMessage().contains("latitude"));
    }

    @Test
    void unknownKeysAreIgnored() throws IOException {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(),
                "latitude=24.7\nlongitude=46.7\nmethod=UMM_AL_QURA\nmadhab=SHAFI\nfuture_key=whatever\n");
        assertTrue(new ConfigStore(configFile()).load().isPresent());
    }

    @Test
    void missingTimeformatDefaultsToTwelveHour() throws IOException {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(),
                "latitude=24.7\nlongitude=46.7\nmethod=UMM_AL_QURA\nmadhab=SHAFI\n");
        assertEquals(TimeFormat.TWELVE_HOUR,
                new ConfigStore(configFile()).load().orElseThrow().timeFormat());
    }

    @Test
    void defaultFileEndsWithAdhanConfig() {
        Path path = ConfigStore.defaultFile();
        assertTrue(path.endsWith(Path.of("adhan", "config")), "was: " + path);
    }
}
