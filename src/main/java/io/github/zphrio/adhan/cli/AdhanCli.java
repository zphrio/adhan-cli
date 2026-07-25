package io.github.zphrio.adhan.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "adhan",
        mixinStandardHelpOptions = true,
        versionProvider = AdhanCli.VersionProvider.class,
        description = "Islamic prayer times for your terminal.")
public class AdhanCli implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static CommandLine buildCommandLine() {
        return new CommandLine(new AdhanCli());
    }

    public static void main(String[] args) {
        System.exit(buildCommandLine().execute(args));
    }

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            try (var in = AdhanCli.class.getResourceAsStream("/version.txt")) {
                return new String[]{"adhan " + new String(in.readAllBytes()).trim()};
            }
        }
    }
}
