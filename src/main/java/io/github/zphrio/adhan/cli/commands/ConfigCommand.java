package io.github.zphrio.adhan.cli.commands;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;
import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import io.github.zphrio.adhan.cli.config.InvalidConfigException;
import io.github.zphrio.adhan.cli.config.TimeFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "config", description = "Configure adhan (interactive wizard).")
public class ConfigCommand implements Callable<Integer> {

    @Option(names = "--list", description = "Show current configuration.")
    boolean list;

    @Spec
    CommandSpec spec;

    private final ConfigStore store;
    private final BufferedReader in;

    public ConfigCommand(ConfigStore store, BufferedReader in) {
        this.store = store;
        this.in = in;
    }

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        if (list) {
            Optional<Config> config = Commands.loadConfig(store, err);
            if (config.isEmpty()) {
                return 2;
            }
            Config c = config.get();
            out.println("latitude=" + c.latitude());
            out.println("longitude=" + c.longitude());
            out.println("method=" + c.method().name());
            out.println("madhab=" + c.madhab().name());
            out.println("timeformat=" + c.timeFormat().key());
            return 0;
        }

        Config existing;
        try {
            existing = store.load().orElse(null);
        } catch (InvalidConfigException e) {
            existing = null; // corrupt config: run the wizard without defaults
        }

        try {
            double[] coordinates = promptCoordinates(out, existing);
            CalculationMethod method = promptMethod(out, existing);
            Madhab madhab = promptMadhab(out, existing);
            TimeFormat timeFormat = promptTimeFormat(out, existing);
            store.save(new Config(coordinates[0], coordinates[1], method, madhab, timeFormat));
            out.println("Configuration saved.");
            return 0;
        } catch (AbortedException e) {
            err.println("Aborted. Configuration unchanged.");
            return 1;
        }
    }

    private String prompt(PrintWriter out, String label, String fallback) throws IOException {
        out.print(fallback == null ? label + ": " : label + " [" + fallback + "]: ");
        out.flush();
        String line = in.readLine();
        if (line == null) {
            throw new AbortedException();
        }
        line = line.trim();
        return (line.isEmpty() && fallback != null) ? fallback : line;
    }

    private double[] promptCoordinates(PrintWriter out, Config existing) throws IOException {
        String fallback = existing == null ? null : existing.latitude() + "," + existing.longitude();
        while (true) {
            String answer = prompt(out, "Coordinates (lat,long)", fallback);
            String[] parts = answer.split(",");
            if (parts.length == 2) {
                try {
                    double latitude = Double.parseDouble(parts[0].trim());
                    double longitude = Double.parseDouble(parts[1].trim());
                    if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                        return new double[] {latitude, longitude};
                    }
                } catch (NumberFormatException ignored) {
                    // fall through to the hint below
                }
            }
            out.println("Enter coordinates as lat,long — e.g. 24.7136,46.6753");
        }
    }

    private CalculationMethod promptMethod(PrintWriter out, Config existing) throws IOException {
        CalculationMethod[] methods = CalculationMethod.values();
        for (int i = 0; i < methods.length; i++) {
            out.println("  " + (i + 1) + ") " + methods[i].name());
        }
        String fallback =
                existing == null ? null : String.valueOf(existing.method().ordinal() + 1);
        while (true) {
            String answer = prompt(out, "Method", fallback);
            try {
                int choice = Integer.parseInt(answer);
                if (choice >= 1 && choice <= methods.length) {
                    return methods[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                // fall through to the hint below
            }
            out.println("Enter a number between 1 and " + methods.length);
        }
    }

    private Madhab promptMadhab(PrintWriter out, Config existing) throws IOException {
        String fallback = existing == null ? null : (existing.madhab() == Madhab.SHAFI ? "1" : "2");
        while (true) {
            String answer = prompt(out, "Madhab (1=SHAFI, 2=HANAFI)", fallback);
            if ("1".equals(answer)) {
                return Madhab.SHAFI;
            }
            if ("2".equals(answer)) {
                return Madhab.HANAFI;
            }
            out.println("Enter 1 or 2");
        }
    }

    private TimeFormat promptTimeFormat(PrintWriter out, Config existing) throws IOException {
        String fallback = (existing == null || existing.timeFormat() == TimeFormat.TWELVE_HOUR) ? "1" : "2";
        while (true) {
            String answer = prompt(out, "Time format (1=12h, 2=24h)", fallback);
            if ("1".equals(answer)) {
                return TimeFormat.TWELVE_HOUR;
            }
            if ("2".equals(answer)) {
                return TimeFormat.TWENTY_FOUR_HOUR;
            }
            out.println("Enter 1 or 2");
        }
    }

    private static final class AbortedException extends RuntimeException {}
}
