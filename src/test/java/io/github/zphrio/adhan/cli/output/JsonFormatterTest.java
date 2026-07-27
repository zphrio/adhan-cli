package io.github.zphrio.adhan.cli.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zphrio.adhan.cli.config.TimeFormat;
import io.github.zphrio.adhan.cli.core.DayTimes;
import io.github.zphrio.adhan.cli.core.NextPrayer;
import io.github.zphrio.adhan.cli.core.PrayerName;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonFormatterTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");
    private static final LocalDate DATE = LocalDate.of(2025, 1, 11);

    private static ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2025, 1, 11, hour, minute, 0, 0, ZONE);
    }

    private static JsonFormatter formatter() {
        return new JsonFormatter(new TextFormatter(TimeFormat.TWELVE_HOUR));
    }

    private static DayTimes sampleDay() {
        Map<PrayerName, ZonedDateTime> times = new EnumMap<>(PrayerName.class);
        times.put(PrayerName.FAJR, at(5, 17));
        times.put(PrayerName.SHUROQ, at(6, 40));
        times.put(PrayerName.DUHR, at(12, 1));
        times.put(PrayerName.ASR, at(15, 3));
        times.put(PrayerName.MAGHRIB, at(17, 23));
        times.put(PrayerName.ISHA, at(18, 53));
        return new DayTimes(DATE, times);
    }

    @Test
    void dayJsonMatchesSchemaExactly() {
        String expected = """
				{
				  "date": "2025-01-11",
				  "timezone": "Asia/Riyadh",
				  "times": {
				    "fajr": {"display": "5:17 AM", "iso": "2025-01-11T05:17:00+03:00"},
				    "shuroq": {"display": "6:40 AM", "iso": "2025-01-11T06:40:00+03:00"},
				    "duhr": {"display": "12:01 PM", "iso": "2025-01-11T12:01:00+03:00"},
				    "asr": {"display": "3:03 PM", "iso": "2025-01-11T15:03:00+03:00"},
				    "maghrib": {"display": "5:23 PM", "iso": "2025-01-11T17:23:00+03:00"},
				    "isha": {"display": "6:53 PM", "iso": "2025-01-11T18:53:00+03:00"}
				  }
				}""";
        assertEquals(expected, formatter().format(sampleDay()));
    }

    @Test
    void dayJsonIsParseable() {
        assertDoesNotThrow(() -> new ObjectMapper().readTree(formatter().format(sampleDay())));
    }

    @Test
    void nextJsonMatchesSchemaExactly() {
        NextPrayer next = new NextPrayer(PrayerName.ASR, at(15, 15), Duration.ofMinutes(85));
        String expected = """
				{
				  "name": "Asr",
				  "key": "asr",
				  "display": "3:15 PM",
				  "iso": "2025-01-11T15:15:00+03:00",
				  "remaining_minutes": 85
				}""";
        assertEquals(expected, formatter().format(next));
    }

    @Test
    void nextJsonFloorsRemainingToWholeMinutes() throws Exception {
        NextPrayer next = new NextPrayer(PrayerName.ASR, at(15, 15), Duration.ofSeconds(85 * 60 + 30));
        JsonNode root = new ObjectMapper().readTree(formatter().format(next));
        assertEquals(85, root.get("remaining_minutes").asInt());
    }
}
