package io.github.zphrio.adhan.cli.output;

import io.github.zphrio.adhan.cli.config.TimeFormat;
import io.github.zphrio.adhan.cli.core.DayTimes;
import io.github.zphrio.adhan.cli.core.NextPrayer;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringJoiner;

public final class TextFormatter {

    private final DateTimeFormatter timeFormatter;

    public TextFormatter(TimeFormat format) {
        this.timeFormatter = format == TimeFormat.TWELVE_HOUR
                ? DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                : DateTimeFormatter.ofPattern("HH:mm", Locale.US);
    }

    public String time(ZonedDateTime t) {
        return timeFormatter.format(t);
    }

    public String format(DayTimes day) {
        StringJoiner lines = new StringJoiner("\n");
        day.times().forEach((name, t) -> lines.add(name.display() + ": " + time(t)));
        return lines.toString();
    }

    public String format(NextPrayer next, boolean withRemaining) {
        String line = next.name().display() + " " + time(next.time());
        return withRemaining ? line + "\nRemaining: " + remaining(next.remaining()) : line;
    }

    private static String remaining(Duration d) {
        long totalMinutes = d.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
}
