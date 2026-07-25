package io.github.zphrio.adhan.cli.config;

public enum TimeFormat {
    TWELVE_HOUR("12h"),
    TWENTY_FOUR_HOUR("24h");

    private final String key;

    TimeFormat(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static TimeFormat fromKey(String key) {
        for (TimeFormat format : values()) {
            if (format.key.equals(key)) {
                return format;
            }
        }
        throw new InvalidConfigException("timeformat must be one of: 12h, 24h (got: " + key + ")");
    }
}
