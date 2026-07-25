package io.github.zphrio.adhan.cli.output;

import io.github.zphrio.adhan.cli.core.DayTimes;
import io.github.zphrio.adhan.cli.core.NextPrayer;
import io.github.zphrio.adhan.cli.core.PrayerName;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringJoiner;

public final class JsonFormatter {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);

    private final TextFormatter display;

    public JsonFormatter(TextFormatter display) {
        this.display = display;
    }

    public String format(DayTimes day) {
        String zone = day.times().get(PrayerName.FAJR).getZone().getId();
        StringJoiner entries = new StringJoiner(",\n");
        day.times().forEach((name, t) -> entries.add(
                "    " + quote(name.key()) + ": {\"display\": " + quote(display.time(t))
                        + ", \"iso\": " + quote(ISO.format(t)) + "}"));
        return "{\n"
                + "  \"date\": " + quote(day.date().toString()) + ",\n"
                + "  \"timezone\": " + quote(zone) + ",\n"
                + "  \"times\": {\n"
                + entries + "\n"
                + "  }\n"
                + "}";
    }

    public String format(NextPrayer next) {
        return "{\n"
                + "  \"name\": " + quote(next.name().display()) + ",\n"
                + "  \"key\": " + quote(next.name().key()) + ",\n"
                + "  \"display\": " + quote(display.time(next.time())) + ",\n"
                + "  \"iso\": " + quote(ISO.format(next.time())) + ",\n"
                + "  \"remaining_minutes\": " + next.remaining().toMinutes() + "\n"
                + "}";
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
