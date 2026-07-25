package io.github.zphrio.adhan.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;
import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import io.github.zphrio.adhan.cli.config.TimeFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdhanCliEndToEndTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2025, 1, 11, 10, 0, 0, 0, ZONE).toInstant(), ZONE);

    @TempDir
    Path tempDir;

    private StringWriter out;
    private StringWriter err;

    private int execute(ConfigStore store, String... args) {
        out = new StringWriter();
        err = new StringWriter();
        CommandLine cmd = AdhanCli.buildCommandLine(store, CLOCK, new BufferedReader(new StringReader("")));
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        return cmd.execute(args);
    }

    private ConfigStore emptyStore() {
        return new ConfigStore(tempDir.resolve("config"));
    }

    private ConfigStore configuredStore() {
        ConfigStore store = emptyStore();
        store.save(new Config(24.71352778, 46.67519444,
                CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWELVE_HOUR));
        return store;
    }

    @Test
    void todayFailsWhenNotConfigured() {
        assertEquals(2, execute(emptyStore(), "today"));
        assertEquals("Not configured. Run: adhan config", err.toString().trim());
        assertEquals("", out.toString());
    }

    @Test
    void nextFailsWhenNotConfigured() {
        assertEquals(2, execute(emptyStore(), "next"));
        assertEquals("Not configured. Run: adhan config", err.toString().trim());
    }

    @Test
    void todayPrintsSixPrayerLines() {
        assertEquals(0, execute(configuredStore(), "today"));
        String[] lines = out.toString().trim().split("\n");
        assertEquals(6, lines.length);
        assertTrue(lines[0].startsWith("Fajr: "));
        assertTrue(lines[1].startsWith("Shuroq: "));
        assertTrue(lines[5].startsWith("Isha: "));
    }

    @Test
    void todayJsonEmitsValidJsonOnCleanStdout() throws Exception {
        assertEquals(0, execute(configuredStore(), "today", "--json"));
        assertEquals("", err.toString());
        JsonNode root = new ObjectMapper().readTree(out.toString());
        assertEquals("2025-01-11", root.get("date").asText());
        assertEquals(6, root.get("times").size());
    }

    @Test
    void nextAtMidMorningIsDuhr() {
        assertEquals(0, execute(configuredStore(), "next"));
        assertTrue(out.toString().startsWith("Duhr "), "was: " + out);
        assertEquals(1, out.toString().trim().split("\n").length);
    }

    @Test
    void nextRemainingAddsSecondLine() {
        assertEquals(0, execute(configuredStore(), "next", "--remaining"));
        String[] lines = out.toString().trim().split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].startsWith("Remaining: "));
    }

    @Test
    void nextJsonAlwaysIncludesRemainingMinutes() throws Exception {
        assertEquals(0, execute(configuredStore(), "next", "--json"));
        JsonNode root = new ObjectMapper().readTree(out.toString());
        assertEquals("duhr", root.get("key").asText());
        assertTrue(root.get("remaining_minutes").asInt() > 0);
    }

    @Test
    void invalidConfigValueFailsNamingTheKey() throws Exception {
        Path file = tempDir.resolve("config");
        Files.writeString(file, "latitude=24.7\nlongitude=46.7\nmethod=BOGUS\nmadhab=SHAFI\n");
        assertEquals(2, execute(new ConfigStore(file), "today"));
        assertTrue(err.toString().contains("method"));
        assertEquals("", out.toString());
    }

    @Test
    void unknownSubcommandExitsWithUsageError() {
        assertEquals(2, execute(emptyStore(), "nope"));
    }
}
