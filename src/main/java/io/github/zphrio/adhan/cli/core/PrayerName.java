package io.github.zphrio.adhan.cli.core;

public enum PrayerName {
    FAJR("Fajr", "fajr"),
    SHUROQ("Shuroq", "shuroq"),
    DUHR("Duhr", "duhr"),
    ASR("Asr", "asr"),
    MAGHRIB("Maghrib", "maghrib"),
    ISHA("Isha", "isha");

    private final String display;
    private final String key;

    PrayerName(String display, String key) {
        this.display = display;
        this.key = key;
    }

    public String display() {
        return display;
    }

    public String key() {
        return key;
    }
}
