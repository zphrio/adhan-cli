package io.github.zphrio.adhan.cli.core;

import java.time.Duration;
import java.time.ZonedDateTime;

public record NextPrayer(PrayerName name, ZonedDateTime time, Duration remaining) {
}
