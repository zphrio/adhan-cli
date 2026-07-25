package io.github.zphrio.adhan.cli.core;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record DayTimes(LocalDate date, Map<PrayerName, ZonedDateTime> times) {
    public DayTimes {
        times = Collections.unmodifiableMap(new EnumMap<>(times));
    }
}
