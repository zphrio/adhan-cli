package io.github.zphrio.adhan.cli.core;

import io.github.zphrio.adhan.CalculationParameters;
import io.github.zphrio.adhan.Coordinates;
import io.github.zphrio.adhan.PrayerTimes;
import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.data.DateComponents;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PrayerSchedule {

    private final Config config;
    private final Clock clock;

    public PrayerSchedule(Config config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    public DayTimes today() {
        LocalDate date = LocalDate.now(clock);
        PrayerTimes pt = timesFor(date);
        Map<PrayerName, ZonedDateTime> times = new EnumMap<>(PrayerName.class);
        times.put(PrayerName.FAJR, toZoned(pt.fajr));
        times.put(PrayerName.SHUROQ, toZoned(pt.sunrise));
        times.put(PrayerName.DUHR, toZoned(pt.dhuhr));
        times.put(PrayerName.ASR, toZoned(pt.asr));
        times.put(PrayerName.MAGHRIB, toZoned(pt.maghrib));
        times.put(PrayerName.ISHA, toZoned(pt.isha));
        return new DayTimes(date, times);
    }

    public NextPrayer next() {
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();
        NextPrayer next = firstAfter(timesFor(today), now);
        return next != null ? next : firstAfter(timesFor(today.plusDays(1)), now);
    }

    // Shuroq is sunrise, not a prayer — deliberately absent here
    private NextPrayer firstAfter(PrayerTimes pt, Instant now) {
        Map<PrayerName, Date> prayers = new LinkedHashMap<>();
        prayers.put(PrayerName.FAJR, pt.fajr);
        prayers.put(PrayerName.DUHR, pt.dhuhr);
        prayers.put(PrayerName.ASR, pt.asr);
        prayers.put(PrayerName.MAGHRIB, pt.maghrib);
        prayers.put(PrayerName.ISHA, pt.isha);
        for (Map.Entry<PrayerName, Date> entry : prayers.entrySet()) {
            Instant time = entry.getValue().toInstant();
            if (time.isAfter(now)) {
                return new NextPrayer(entry.getKey(), toZoned(entry.getValue()), Duration.between(now, time));
            }
        }
        return null;
    }

    private PrayerTimes timesFor(LocalDate date) {
        Coordinates coordinates = new Coordinates(config.latitude(), config.longitude());
        CalculationParameters parameters = config.method().getParameters();
        parameters.madhab = config.madhab();
        DateComponents dateComponents =
                new DateComponents(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        return new PrayerTimes(coordinates, dateComponents, parameters);
    }

    private ZonedDateTime toZoned(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), clock.getZone());
    }
}
