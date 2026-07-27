package io.github.zphrio.adhan.cli.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;
import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.TimeFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PrayerScheduleTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");
    private static final LocalDate DATE = LocalDate.of(2025, 1, 11);
    // Official Umm al-Qura times for Riyadh on 2025-01-11 (variance ±2 min):
    // Fajr 5:17 AM, Shuroq 6:40 AM, Duhr 12:01 PM, Asr 3:03 PM, Maghrib 5:23 PM, Isha 6:53 PM
    private static final Config RIYADH =
            new Config(24.71352778, 46.67519444, CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWELVE_HOUR);

    private static Instant instantAt(int hour, int minute) {
        return ZonedDateTime.of(DATE, LocalTime.of(hour, minute), ZONE).toInstant();
    }

    private static PrayerSchedule scheduleAt(int hour, int minute) {
        return new PrayerSchedule(RIYADH, Clock.fixed(instantAt(hour, minute), ZONE));
    }

    private static void assertCloseTo(ZonedDateTime actual, LocalDate date, String expected) {
        ZonedDateTime expectedTime = ZonedDateTime.of(
                date, LocalTime.parse(expected, DateTimeFormatter.ofPattern("h:mm a", Locale.US)), ZONE);
        long diffMinutes = Math.abs(Duration.between(expectedTime, actual).toMinutes());
        assertTrue(diffMinutes <= 2, "expected ~" + expectedTime + " but was " + actual);
    }

    @Test
    void todayReturnsAllSixTimes() {
        DayTimes day = scheduleAt(10, 0).today();
        assertEquals(DATE, day.date());
        assertCloseTo(day.times().get(PrayerName.FAJR), DATE, "5:17 AM");
        assertCloseTo(day.times().get(PrayerName.SHUROQ), DATE, "6:40 AM");
        assertCloseTo(day.times().get(PrayerName.DUHR), DATE, "12:01 PM");
        assertCloseTo(day.times().get(PrayerName.ASR), DATE, "3:03 PM");
        assertCloseTo(day.times().get(PrayerName.MAGHRIB), DATE, "5:23 PM");
        assertCloseTo(day.times().get(PrayerName.ISHA), DATE, "6:53 PM");
    }

    @Test
    void todayTimesIterateInPrayerOrder() {
        DayTimes day = scheduleAt(10, 0).today();
        assertEquals(List.of(PrayerName.values()), List.copyOf(day.times().keySet()));
    }

    @Test
    void nextBeforeFajrIsFajr() {
        assertEquals(PrayerName.FAJR, scheduleAt(4, 0).next().name());
    }

    @Test
    void nextBetweenFajrAndShuroqSkipsShuroq() {
        // 6:00 AM: Fajr (5:17) has passed, Shuroq (6:40) is upcoming but is not a prayer
        assertEquals(PrayerName.DUHR, scheduleAt(6, 0).next().name());
    }

    @Test
    void nextMidMorningIsDuhrWithExactRemaining() {
        NextPrayer next = scheduleAt(10, 0).next();
        assertEquals(PrayerName.DUHR, next.name());
        assertCloseTo(next.time(), DATE, "12:01 PM");
        assertEquals(Duration.between(instantAt(10, 0), next.time().toInstant()), next.remaining());
    }

    @Test
    void nextExactlyAtPrayerTimeMovesToTheFollowingPrayer() {
        ZonedDateTime duhr = scheduleAt(10, 0).today().times().get(PrayerName.DUHR);
        PrayerSchedule atDuhr = new PrayerSchedule(RIYADH, Clock.fixed(duhr.toInstant(), ZONE));
        assertEquals(PrayerName.ASR, atDuhr.next().name());
    }

    @Test
    void nextAfterIshaRollsToTomorrowsFajr() {
        NextPrayer next = scheduleAt(23, 30).next();
        assertEquals(PrayerName.FAJR, next.name());
        assertEquals(DATE.plusDays(1), next.time().toLocalDate());
        // Fajr drifts well under a minute per day in January — compare to the 11th's value, looser tolerance
        ZonedDateTime approx = ZonedDateTime.of(DATE.plusDays(1), LocalTime.of(5, 17), ZONE);
        assertTrue(Math.abs(Duration.between(approx, next.time()).toMinutes()) <= 5);
        assertTrue(next.remaining().toHours() >= 5);
    }
}
