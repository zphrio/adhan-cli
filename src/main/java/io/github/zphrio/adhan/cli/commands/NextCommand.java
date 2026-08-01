package io.github.zphrio.adhan.cli.commands;

import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import io.github.zphrio.adhan.cli.core.NextPrayer;
import io.github.zphrio.adhan.cli.core.PrayerSchedule;
import io.github.zphrio.adhan.cli.output.JsonFormatter;
import io.github.zphrio.adhan.cli.output.TextFormatter;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "next", description = "Show the next prayer.")
public class NextCommand implements Callable<Integer> {

    @Option(
            names = {"-r", "--remaining"},
            description = "Also show remaining time until the next prayer.")
    boolean remaining;

    @Option(names = "--json", description = "Output as JSON (always includes remaining_minutes).")
    boolean json;

    @Spec
    CommandSpec spec;

    private final ConfigStore store;
    private final Clock clock;

    public NextCommand(ConfigStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public Integer call() {
        Optional<Config> config = Commands.loadConfig(store, spec.commandLine().getErr());
        if (config.isEmpty()) {
            return 2;
        }
        NextPrayer next = new PrayerSchedule(config.get(), clock).next();
        TextFormatter text = new TextFormatter(config.get().timeFormat());
        String output = json ? new JsonFormatter(text).format(next) : text.format(next, remaining);
        spec.commandLine().getOut().println(output);
        return 0;
    }
}
