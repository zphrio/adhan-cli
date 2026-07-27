package io.github.zphrio.adhan.cli.config;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;

public record Config(
        double latitude, double longitude, CalculationMethod method, Madhab madhab, TimeFormat timeFormat) {

    public Config {
        if (!(latitude >= -90 && latitude <= 90)) {
            throw new InvalidConfigException("latitude must be between -90 and 90 (got: " + latitude + ")");
        }
        if (!(longitude >= -180 && longitude <= 180)) {
            throw new InvalidConfigException("longitude must be between -180 and 180 (got: " + longitude + ")");
        }
        if (method == null) {
            throw new InvalidConfigException("method is required");
        }
        if (madhab == null) {
            throw new InvalidConfigException("madhab is required");
        }
        if (timeFormat == null) {
            timeFormat = TimeFormat.TWELVE_HOUR;
        }
    }
}
