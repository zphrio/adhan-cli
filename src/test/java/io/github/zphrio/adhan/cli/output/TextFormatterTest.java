package io.github.zphrio.adhan.cli.output;

import io.github.zphrio.adhan.cli.config.TimeFormat;
import io.github.zphrio.adhan.cli.core.DayTimes;
import io.github.zphrio.adhan.cli.core.NextPrayer;
import io.github.zphrio.adhan.cli.core.PrayerName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatterTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");
    private static final LocalDate DATE = LocalDate.of(2025, 1, 11);

    private static ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2025, 1, 11, hour, minute, 0, 0, ZONE);
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
    void twelveHourDayOutput() {
        String expected = """
                Fajr: 5:17 AM
                Shuroq: 6:40 AM
                Duhr: 12:01 PM
                Asr: 3:03 PM
                Maghrib: 5:23 PM
                Isha: 6:53 PM""";
        assertEquals(expected, new TextFormatter(TimeFormat.TWELVE_HOUR).format(sampleDay()));
    }

    @Test
    void twentyFourHourZeroPads() {
        assertEquals("05:17", new TextFormatter(TimeFormat.TWENTY_FOUR_HOUR).time(at(5, 17)));
        assertEquals("18:53", new TextFormatter(TimeFormat.TWENTY_FOUR_HOUR).time(at(18, 53)));
    }

    @Test
    void nextWithoutRemaining() {
        NextPrayer next = new NextPrayer(PrayerName.ASR, at(15, 15), Duration.ofMinutes(85));
        assertEquals("Asr 3:15 PM", new TextFormatter(TimeFormat.TWELVE_HOUR).format(next, false));
    }

    @Test
    void nextWithRemainingOverAnHour() {
        NextPrayer next = new NextPrayer(PrayerName.ASR, at(15, 15), Duration.ofMinutes(85));
        assertEquals("Asr 3:15 PM\nRemaining: 1h 25m",
                new TextFormatter(TimeFormat.TWELVE_HOUR).format(next, true));
    }

    @Test
    void nextWithRemainingUnderAnHour() {
        NextPrayer next = new NextPrayer(PrayerName.ASR, at(15, 15), Duration.ofMinutes(35));
        assertEquals("Asr 3:15 PM\nRemaining: 35m",
                new TextFormatter(TimeFormat.TWELVE_HOUR).format(next, true));
    }

    @Test
    void remainingFloorsSeconds() {
        NextPrayer next = new NextPrayer(PrayerName.ASR, at(15, 15), Duration.ofSeconds(35 * 60 + 40));
        assertEquals("Asr 3:15 PM\nRemaining: 35m",
                new TextFormatter(TimeFormat.TWELVE_HOUR).format(next, true));
    }
}
