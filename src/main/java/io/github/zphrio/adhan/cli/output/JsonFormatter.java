package io.github.zphrio.adhan.cli.output;

import io.github.zphrio.adhan.cli.core.DayTimes;
import io.github.zphrio.adhan.cli.core.NextPrayer;
import io.github.zphrio.adhan.cli.core.PrayerName;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringJoiner;

public final class JsonFormatter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);

    private final TextFormatter display;

    public JsonFormatter(TextFormatter display) {
        this.display = display;
    }

    public String format(DayTimes day) {
        String zone = day.times().get(PrayerName.FAJR).getZone().getId();
        StringJoiner entries = new StringJoiner(",\n");
        day.times()
                .forEach((name, t) -> entries.add("    %s: {\"display\": %s, \"iso\": %s}"
                        .formatted(quote(name.key()), quote(display.time(t)), quote(ISO.format(t)))));
        return """
				{
				  "date": %s,
				  "timezone": %s,
				  "times": {
				%s
				  }
				}""".formatted(quote(day.date().toString()), quote(zone), entries);
    }

    public String format(NextPrayer next) {
        return """
				{
				  "name": %s,
				  "key": %s,
				  "display": %s,
				  "iso": %s,
				  "remaining_minutes": %d
				}""".formatted(
                        quote(next.name().display()),
                        quote(next.name().key()),
                        quote(display.time(next.time())),
                        quote(ISO.format(next.time())),
                        next.remaining().toMinutes());
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
