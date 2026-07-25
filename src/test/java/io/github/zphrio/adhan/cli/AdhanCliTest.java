package io.github.zphrio.adhan.cli;

import io.github.zphrio.adhan.cli.config.ConfigStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdhanCliTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine(StringWriter out) {
        CommandLine cmd = AdhanCli.buildCommandLine(
                new ConfigStore(tempDir.resolve("config")),
                Clock.systemDefaultZone(),
                new BufferedReader(new StringReader("")));
        cmd.setOut(new PrintWriter(out));
        return cmd;
    }

    @Test
    void versionPrintsNameAndVersion() {
        StringWriter out = new StringWriter();
        int exit = commandLine(out).execute("--version");
        assertEquals(0, exit);
        assertEquals("adhan 0.1", out.toString().trim());
    }

    @Test
    void bareInvocationPrintsHelp() {
        StringWriter out = new StringWriter();
        int exit = commandLine(out).execute();
        assertEquals(0, exit);
        assertTrue(out.toString().contains("Usage: adhan"));
    }
}
