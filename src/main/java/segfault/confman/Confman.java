package segfault.confman;

import segfault.confman.confpkg.ConfItem;
import segfault.confman.confpkg.ConfItemEnvironment;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class Confman {
    public static void main(@Nullable String... args) throws Throwable {
        GlobalConfig.set(new GlobalConfig(System.getenv().containsKey("DEBUG")));
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Debugging enabled.");
        }
        if (args == null || args.length < 1) {
            System.err.println("Error: invalid number of args.");
            System.exit(1);
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Args: " + Arrays.toString(args));
        }

        boolean dryRun = false;
        boolean skipCheck = false;
        final String rawPath = args[args.length - 1];

        for (final String arg : args) {
            switch (arg) {
                case "--dry-run":
                    dryRun = true;
                    break;
                case "--skip-check":
                    skipCheck = true;
                    break;
                default:
                    if (arg.equals(rawPath)) break;
                    System.err.println("Unknown argument: " + arg);
                    System.exit(1);
                    return;
            }
        }

        final File start = new File(rawPath);
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Starting at: " + start);
        }

        final List<ConfItem> tasks = Files.walk(start.toPath())
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(file -> file.getName().toLowerCase().matches("\\d\\d-.*\\.ini"))
                .map(file -> {
                    try {
                        if (GlobalConfig.get().DEBUG) {
                            System.out.println();
                            System.out.println("Resolving " + file);
                        }
                        return ConfItemEnvironment.resolve(file, start);
                    } catch (IOException | IllegalStateException e) {
                        System.err.println("Cannot resolve " + file.getAbsolutePath() + ": " + e.getMessage());
                        System.exit(1);
                        throw new RuntimeException();
                    }
                })
                .map(ConfItem::new)
                .sorted(Comparator.comparing(ConfItem::toString))
                .collect(Collectors.toList());
        if (GlobalConfig.get().DEBUG) {
            System.out.println();
            System.out.println("Tasks:");
            System.out.println(tasks);
        }

        for (final ConfItem item : tasks) {
            if (GlobalConfig.get().DEBUG) {
                System.out.println();
                System.out.println("=== Running " + item.toString() + " ===");
            }
            int r;
            // Conditions
            if (GlobalConfig.get().DEBUG) {
                System.out.println("Verifying conditions...");
            }
            r = item.verify();
            if (r != 0) {
                System.err.println("Failed verifying " + item.toString());
                System.exit(0);
            }

            // Check
            if (GlobalConfig.get().DEBUG) {
                System.out.println("Checking...");
            }
            if (!skipCheck) {
                r = item.check();
                if (r != 0) {
                    System.err.println("Failed checking " + item.toString());
                    System.exit(r);
                    return;
                }
            }

            if (!dryRun) {
                // Pre-hook
                if (GlobalConfig.get().DEBUG) {
                    System.out.println("Running pre-exec hook...");
                }
                r = item.before();
                if (r != 0) {
                    System.err.println("Failed running pre-exec hook of " + item.toString());
                    System.exit(r);
                    return;
                }

                // Run
                if (GlobalConfig.get().DEBUG) {
                    System.out.println("Running...");
                }
                r = item.run();
                if (r != 0) {
                    System.err.println("Failed to run " + item.toString());
                    System.exit(r);
                    return;
                }

                // Post-hook
                if (GlobalConfig.get().DEBUG) {
                    System.out.println("Running post-exec hook...");
                }
                r = item.after();
                if (r != 0) {
                    System.err.println("Failed running post-exec hook of " + item.toString());
                    System.exit(r);
                    return;
                }
            }

            if (GlobalConfig.get().DEBUG) {
                System.out.println("Done.");
            }
        }
    }
}
