package io.github.zphrio.adhan.cli.config;

import io.github.zphrio.adhan.CalculationMethod;
import io.github.zphrio.adhan.Madhab;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void validConfigIsCreated() {
        Config config = new Config(24.7136, 46.6753,
                CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWELVE_HOUR);
        assertEquals(24.7136, config.latitude());
        assertEquals(TimeFormat.TWELVE_HOUR, config.timeFormat());
    }

    @Test
    void latitudeOutOfRangeThrows() {
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new Config(90.5, 46.6753, CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWELVE_HOUR));
        assertTrue(ex.getMessage().contains("latitude"));
    }

    @Test
    void longitudeOutOfRangeThrows() {
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new Config(24.7136, -180.1, CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, TimeFormat.TWELVE_HOUR));
        assertTrue(ex.getMessage().contains("longitude"));
    }

    @Test
    void nullMethodThrows() {
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new Config(24.7136, 46.6753, null, Madhab.SHAFI, TimeFormat.TWELVE_HOUR));
        assertTrue(ex.getMessage().contains("method"));
    }

    @Test
    void nullMadhabThrows() {
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> new Config(24.7136, 46.6753, CalculationMethod.UMM_AL_QURA, null, TimeFormat.TWELVE_HOUR));
        assertTrue(ex.getMessage().contains("madhab"));
    }

    @Test
    void nullTimeFormatDefaultsToTwelveHour() {
        Config config = new Config(24.7136, 46.6753,
                CalculationMethod.UMM_AL_QURA, Madhab.SHAFI, null);
        assertEquals(TimeFormat.TWELVE_HOUR, config.timeFormat());
    }

    @Test
    void timeFormatFromKey() {
        assertEquals(TimeFormat.TWELVE_HOUR, TimeFormat.fromKey("12h"));
        assertEquals(TimeFormat.TWENTY_FOUR_HOUR, TimeFormat.fromKey("24h"));
    }

    @Test
    void timeFormatFromUnknownKeyThrows() {
        InvalidConfigException ex = assertThrows(InvalidConfigException.class,
                () -> TimeFormat.fromKey("13h"));
        assertTrue(ex.getMessage().contains("timeformat"));
        assertTrue(ex.getMessage().contains("12h"));
    }
}
