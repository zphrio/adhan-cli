package io.github.zphrio.adhan.cli.commands;

import io.github.zphrio.adhan.cli.config.Config;
import io.github.zphrio.adhan.cli.config.ConfigStore;
import io.github.zphrio.adhan.cli.config.InvalidConfigException;
import java.io.PrintWriter;
import java.util.Optional;

final class Commands {

    private Commands() {}

    /** Loads config, printing the appropriate one-line error to err when it can't. */
    static Optional<Config> loadConfig(ConfigStore store, PrintWriter err) {
        try {
            Optional<Config> config = store.load();
            if (config.isEmpty()) {
                err.println("Not configured. Run: adhan config");
            }
            return config;
        } catch (InvalidConfigException e) {
            err.println(e.getMessage());
            return Optional.empty();
        }
    }
}
