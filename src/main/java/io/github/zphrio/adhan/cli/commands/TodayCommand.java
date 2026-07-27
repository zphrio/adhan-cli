package io.github.zphrio.adhan.cli.commands;

import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import io.github.zphrio.adhan.cli.core.DayTimes;
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

@Command(name = "today", description = "Show today's prayer times.")
public class TodayCommand implements Callable<Integer> {

    @Option(names = "--json", description = "Output as JSON.")
    boolean json;

    @Spec
    CommandSpec spec;

    private final ConfigStore store;
    private final Clock clock;

    public TodayCommand(ConfigStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public Integer call() {
        Optional<Config> config = Commands.loadConfig(store, spec.commandLine().getErr());
        if (config.isEmpty()) {
            return 2;
        }
        DayTimes day = new PrayerSchedule(config.get(), clock).today();
        TextFormatter text = new TextFormatter(config.get().timeFormat());
        String output = json ? new JsonFormatter(text).format(day) : text.format(day);
        spec.commandLine().getOut().println(output);
        return 0;
    }
}
