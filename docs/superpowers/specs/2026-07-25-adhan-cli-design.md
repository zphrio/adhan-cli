# Adhan CLI — Design

**Date:** 2026-07-25
**Status:** Approved pending user review

## Overview

`adhan` is a small, fast CLI for Islamic prayer times, for direct terminal use and as a data source for scripts and status bars (e.g. Waybar) on Linux and macOS. It is a thin front-end over the [`io.github.zphrio:adhan-java:1.0`](https://github.com/zphrio/adhan-java) library (published on Maven Central), which performs all prayer-time calculations.

It ships as a self-contained GraalVM native binary: ~10ms startup, no JVM required on the user's machine.

## Decisions

| Decision | Choice |
| --- | --- |
| Runtime | GraalVM native-image binary; JVM only used during development |
| Stack | Java 25, Gradle (Groovy DSL), Picocli, GraalVM Native Build Tools |
| Output | Human text by default; generic `--json` flag (not bound to Waybar or any consumer) |
| Config UX | Single `adhan config` command (wizard); no separate `setup` |
| Location | Manual `lat,long` entry only |
| Settings (v1) | `latitude`, `longitude`, `method`, `madhab`, `timeformat` |
| JSON serialization | Hand-rolled `StringBuilder` writer — no Jackson/Gson in the binary |
| CI/CD, release, README | Deferred (see Deferred work) |

## Architecture

Repo `zphrio/adhan-cli` at `~/IdeaProjects/adhan-cli`. Binary name: `adhan`. License: MIT.

Package `io.github.zphrio.adhan.cli`:

```
src/main/java/io/github/zphrio/adhan/cli/
├── AdhanCli.java              picocli root command + main()
├── commands/
│   ├── ConfigCommand.java
│   ├── TodayCommand.java
│   └── NextCommand.java
├── config/
│   ├── Config.java            immutable, validated settings (record)
│   └── ConfigStore.java       file I/O + XDG path resolution
├── core/
│   ├── PrayerSchedule.java    the only class that touches adhan-java
│   ├── DayTimes.java          value object
│   └── NextPrayer.java        value object
└── output/
    ├── TextFormatter.java
    └── JsonFormatter.java
```

Data flow is one direction: `command → ConfigStore → PrayerSchedule → value object → formatter → stdout`. Each layer only knows the one below it.

Unit responsibilities:

- **`config/`** — knows nothing about prayers. `Config` is a record whose compact constructor validates ranges/enums, so an invalid `Config` cannot exist. `ConfigStore` translates to/from `java.util.Properties` and takes the config file path as a constructor argument (tests point it at a temp dir).
- **`core/`** — `PrayerSchedule` wraps the library and fully contains its legacy `java.util.Date` API, exposing `java.time` types outward. It takes an injected `java.time.Clock`, making all time-dependent logic testable. `PrayerName` is our own display enum (`FAJR, SHUROQ, DUHR, ASR, MAGHRIB, ISHA`) mapped from the library's `Prayer` enum, so library spellings (`SUNRISE`, `DHUHR`) never leak into output.
- **`commands/`** — thin picocli glue (~30 lines each); no logic. A shared helper performs the not-configured check.
- **`output/`** — pure formatting. `TextFormatter` and `JsonFormatter` are constructed with the `TimeFormat` and render `DayTimes` / `NextPrayer`.

Key signatures:

```java
public record Config(double latitude, double longitude,
                     CalculationMethod method, Madhab madhab, TimeFormat timeFormat) {}

public final class ConfigStore {
    public Optional<Config> load();   // empty = not configured
    public void save(Config config);  // atomic: temp file + move
}

public final class PrayerSchedule {  // constructed with Config + Clock
    public DayTimes today();
    public NextPrayer next();
}

public record DayTimes(LocalDate date, Map<PrayerName, ZonedDateTime> times) {}
public record NextPrayer(PrayerName name, ZonedDateTime time, Duration remaining) {}
```

## Commands

- `adhan` (bare) → help. `--help` / `--version` are standard picocli; `--version` prints `adhan <version>` from `gradle.properties`.
- **`adhan config`** — interactive wizard, in order:
  1. Coordinates as one line: `lat,long` (e.g. `24.7136,46.6753`)
  2. Calculation method — numbered list of the library's `CalculationMethod` values
  3. Madhab — `SHAFI` or `HANAFI`
  4. Time format — `12h` or `24h` (default `12h`)

  If a config exists, current values are offered as defaults. Invalid input re-prompts. Ctrl-C/EOF aborts without touching the existing config. `adhan config --list` prints current settings in `key=value` form; if not configured it fails like any other command (`Not configured. Run: adhan config`, exit 2).
- **`adhan today [--json]`** — the day's six times in order: Fajr, Shuroq, Duhr, Asr, Maghrib, Isha.
- **`adhan next [--remaining] [--json]`** — the next prayer. **Shuroq is excluded** (it is sunrise, not a prayer; it appears only in `today`). After Isha, `next` rolls over to tomorrow's Fajr. "Next" means the first prayer whose time is strictly after now — at exactly Asr time, the next prayer is Maghrib. `--remaining` adds remaining time in text mode; JSON always includes `remaining_minutes`.

Dates come from the system clock; times are computed for the system timezone.

## Config file

Path: `$XDG_CONFIG_HOME/adhan/config`, fallback `~/.config/adhan/config`. Plain `key=value`, parsed with `java.util.Properties`:

```properties
latitude=24.7136
longitude=46.6753
method=UMM_AL_QURA
madhab=SHAFI
timeformat=12h
```

Validation rules:

- `latitude` ∈ [-90, 90]; `longitude` ∈ [-180, 180]
- `method` — any `CalculationMethod` enum name
- `madhab` — `SHAFI` or `HANAFI`
- `timeformat` — `12h` or `24h`; missing key defaults to `12h`
- A missing required key or invalid value → one-line error naming the key and listing valid values, exit 2
- Unknown keys are ignored (forward compatibility with future keys)

## Output formats

Text — 12h renders `5:55 AM` (no zero-pad), 24h renders `05:55` (zero-padded):

```
$ adhan today            $ adhan next             $ adhan next --remaining
Fajr: 5:55 AM            Asr 3:15 PM              Asr 3:15 PM
Shuroq: 6:30 AM                                   Remaining: 1h 25m
Duhr: 12:05 PM
Asr: 3:55 PM
Maghrib: 6:33 PM
Isha: 8:25 PM
```

Remaining renders as `1h 25m`, or `35m` when under an hour (minutes floored).

JSON — pretty-printed, 2-space indent. Every time has a `display` string (respects `timeformat`) and an `iso` timestamp with offset:

```json
$ adhan today --json
{
  "date": "2026-07-25",
  "timezone": "Asia/Riyadh",
  "times": {
    "fajr":    {"display": "5:55 AM",  "iso": "2026-07-25T05:55:00+03:00"},
    "shuroq":  {"display": "6:30 AM",  "iso": "2026-07-25T06:30:00+03:00"},
    "duhr":    {"display": "12:05 PM", "iso": "2026-07-25T12:05:00+03:00"},
    "asr":     {"display": "3:55 PM",  "iso": "2026-07-25T15:55:00+03:00"},
    "maghrib": {"display": "6:33 PM",  "iso": "2026-07-25T18:33:00+03:00"},
    "isha":    {"display": "8:25 PM",  "iso": "2026-07-25T20:25:00+03:00"}
  }
}

$ adhan next --json
{
  "name": "Asr",
  "key": "asr",
  "display": "3:15 PM",
  "iso": "2026-07-25T15:15:00+03:00",
  "remaining_minutes": 85
}
```

## Error handling

- Exit codes: `0` success · `1` unexpected runtime error · `2` user/config error (not configured, invalid config, bad CLI usage — picocli usage errors also map to 2).
- Not configured: `Not configured. Run: adhan config` on stderr, exit 2.
- All errors are one-line plain text on stderr — including in `--json` mode. stdout carries clean JSON or nothing; consumers branch on exit code.

## Testing

JUnit 5. Tests run on the JVM; test-only dependencies (e.g. Jackson for parsing JSON output) never enter the native binary.

- **`ConfigStore`** — temp dir: missing file → empty, save/load roundtrip, invalid value errors naming the key, unknown keys ignored, atomic save.
- **`PrayerSchedule`** — fixed `Clock`, Riyadh coordinates with Umm al-Qura, sanity-checked against the Riyadh test data in the adhan-java fork. Boundaries: before Fajr, between prayers, exactly at a prayer time, after Isha → tomorrow's Fajr. Shuroq exclusion.
- **Formatters** — 12h/24h rendering; JSON parsed with a test-only parser to prove validity.
- **Commands** — end-to-end via `CommandLine.execute()` with a temp config dir: exit codes, stderr wording, stdout purity in `--json` mode.
- **Native smoke test (manual, local)** — compile with `./gradlew nativeCompile`, pre-write a config, run the binary directly: `adhan --version`, `adhan today`, `adhan next --json` must exit 0 with expected output.

## Build & local verification

- Gradle plugins: `java`, `application`, GraalVM Native Build Tools. Picocli's annotation processor generates the native-image reflection config.
- Dev loop: `./gradlew run --args="next --remaining"` (JVM, fast iteration).
- Native build: `./gradlew nativeCompile` → `build/native/nativeCompile/adhan`. Requires a local GraalVM JDK with `native-image`.
- Version single-sourced in `gradle.properties`, starting at `0.1` (two-segment `x.x` style). The `generateVersionSource` task bakes it into a generated `BuildInfo.VERSION` constant, which `@Command(version = ...)` uses directly — no resource file, no runtime lookup. Keeping the version in `gradle.properties` (rather than `build.gradle`) preserves `-Pversion=` overrides for tag-driven releases.

No CI in v1 — all verification is local (tests + JVM run + native binary smoke test).

## Deferred work

Explicitly out of scope for v1, in rough priority order:

- **CI/CD** — GitHub Actions: test + native build on push (Linux + macOS matrix)
- **Release automation** — tag-triggered workflow producing `adhan-linux-x86_64` / `adhan-macos-arm64` + SHA-256 checksums on GitHub Releases
- **README** — install, usage, config reference
- **Per-prayer minute adjustments** — config keys mapping to the library's `PrayerAdjustments` (e.g. Umm al-Qura +30 min Isha in Ramadan)
- **High-latitude rule** — expose the library's `HighLatitudeRule`
- **Location lookup** — IP-based auto-detect and/or city-name geocoding in the wizard
- **Package managers** — Homebrew tap, AUR, Fedora COPR, Debian
- **Waybar preset** — documented recipe (or subcommand) emitting Waybar's `text`/`tooltip` schema
