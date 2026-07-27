package io.github.zphrio.adhan.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;
import io.github.zphrio.adhan.cli.AdhanCli;
import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import io.github.zphrio.adhan.cli.config.TimeFormat;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ConfigCommandTest {

    private static final int UMM_AL_QURA_CHOICE = CalculationMethod.UMM_AL_QURA.ordinal() + 1;

    @TempDir
    Path tempDir;

    private StringWriter out;
    private StringWriter err;

    private int execute(ConfigStore store, String stdin, String... args) {
        out = new StringWriter();
        err = new StringWriter();
        CommandLine cmd = AdhanCli.buildCommandLine(
                store, Clock.systemDefaultZone(), new BufferedReader(new StringReader(stdin)));
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        return cmd.execute(args);
    }

    private ConfigStore store() {
        return new ConfigStore(tempDir.resolve("config"));
    }

    @Test
    void wizardSavesConfig() {
        ConfigStore store = store();
        String stdin = "24.7136,46.6753\n" + UMM_AL_QURA_CHOICE + "\n1\n1\n";
        assertEquals(0, execute(store, stdin, "config"));
        Config saved = store.load().orElseThrow();
        assertEquals(24.7136, saved.latitude());
        assertEquals(46.6753, saved.longitude());
        assertEquals(CalculationMethod.UMM_AL_QURA, saved.method());
        assertEquals(Madhab.SHAFI, saved.madhab());
        assertEquals(TimeFormat.TWELVE_HOUR, saved.timeFormat());
        assertTrue(out.toString().contains("Configuration saved."));
    }

    @Test
    void wizardDefaultsTimeFormatToTwelveHourOnEmptyInput() {
        ConfigStore store = store();
        String stdin = "24.7136,46.6753\n" + UMM_AL_QURA_CHOICE + "\n2\n\n";
        assertEquals(0, execute(store, stdin, "config"));
        Config saved = store.load().orElseThrow();
        assertEquals(Madhab.HANAFI, saved.madhab());
        assertEquals(TimeFormat.TWELVE_HOUR, saved.timeFormat());
    }

    @Test
    void wizardRepromptsOnInvalidCoordinates() {
        ConfigStore store = store();
        String stdin = "garbage\n95,46.7\n24.7136,46.6753\n" + UMM_AL_QURA_CHOICE + "\n1\n1\n";
        assertEquals(0, execute(store, stdin, "config"));
        assertTrue(out.toString().contains("Enter coordinates as lat,long"));
        assertEquals(24.7136, store.load().orElseThrow().latitude());
    }

    @Test
    void wizardRepromptsOnInvalidMethodNumber() {
        ConfigStore store = store();
        String stdin = "24.7136,46.6753\n99\n" + UMM_AL_QURA_CHOICE + "\n1\n1\n";
        assertEquals(0, execute(store, stdin, "config"));
        assertEquals(CalculationMethod.UMM_AL_QURA, store.load().orElseThrow().method());
    }

    @Test
    void eofAbortsWithoutSaving() {
        ConfigStore store = store();
        assertEquals(1, execute(store, "24.7136,46.6753\n", "config"));
        assertTrue(err.toString().contains("Aborted"));
        assertTrue(store.load().isEmpty());
    }

    @Test
    void emptyInputsKeepExistingValues() {
        ConfigStore store = store();
        Config existing = new Config(
                24.71352778, 46.67519444, CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWENTY_FOUR_HOUR);
        store.save(existing);
        assertEquals(0, execute(store, "\n\n\n\n", "config"));
        assertEquals(existing, store.load().orElseThrow());
    }

    @Test
    void listPrintsKeyValueLines() {
        ConfigStore store = store();
        store.save(new Config(24.7136, 46.6753, CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWELVE_HOUR));
        assertEquals(0, execute(store, "", "config", "--list"));
        String expected = """
				latitude=24.7136
				longitude=46.6753
				method=UMM_AL_QURA
				madhab=SHAFI
				timeformat=12h""";
        assertEquals(expected, out.toString().trim());
    }

    @Test
    void listFailsWhenNotConfigured() {
        assertEquals(2, execute(store(), "", "config", "--list"));
        assertEquals("Not configured. Run: adhan config", err.toString().trim());
    }
}
