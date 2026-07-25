package io.github.zphrio.adhan.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdhanCliTest {

    @Test
    void versionPrintsNameAndVersion() {
        StringWriter out = new StringWriter();
        CommandLine cmd = AdhanCli.buildCommandLine();
        cmd.setOut(new PrintWriter(out));
        int exit = cmd.execute("--version");
        assertEquals(0, exit);
        assertEquals("adhan 0.1.0", out.toString().trim());
    }

    @Test
    void bareInvocationPrintsHelp() {
        StringWriter out = new StringWriter();
        CommandLine cmd = AdhanCli.buildCommandLine();
        cmd.setOut(new PrintWriter(out));
        int exit = cmd.execute();
        assertEquals(0, exit);
        assertTrue(out.toString().contains("Usage: adhan"));
    }
}
