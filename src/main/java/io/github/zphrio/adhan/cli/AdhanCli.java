package io.github.zphrio.adhan.cli;

import io.github.zphrio.adhan.cli.commands.ConfigCommand;
import io.github.zphrio.adhan.cli.commands.NextCommand;
import io.github.zphrio.adhan.cli.commands.TodayCommand;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Clock;

@Command(name = "adhan",
        mixinStandardHelpOptions = true,
        versionProvider = AdhanCli.VersionProvider.class,
        description = "Islamic prayer times for your terminal.")
public class AdhanCli implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static CommandLine buildCommandLine(ConfigStore store, Clock clock, BufferedReader stdin) {
        return new CommandLine(new AdhanCli())
                .addSubcommand("config", new ConfigCommand(store, stdin))
                .addSubcommand("today", new TodayCommand(store, clock))
                .addSubcommand("next", new NextCommand(store, clock))
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    cmd.getErr().println("Error: " + ex.getMessage());
                    return 1;
                });
    }

    public static void main(String[] args) {
        CommandLine cmd = buildCommandLine(
                new ConfigStore(ConfigStore.defaultFile()),
                Clock.systemDefaultZone(),
                new BufferedReader(new InputStreamReader(System.in)));
        System.exit(cmd.execute(args));
    }

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            try (var in = AdhanCli.class.getResourceAsStream("/version.txt")) {
                return new String[]{"adhan " + new String(in.readAllBytes()).trim()};
            }
        }
    }
}
