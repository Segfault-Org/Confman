package segfault.confman;

import segfault.confman.confpkg.ConfItem;
import segfault.confman.confpkg.ConfItemEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class Confman {
    private static final GlobalResultReport mReport = new GlobalResultReport();
    public static void main(@Nullable String... args) throws Throwable {
        GlobalConfig.set(new GlobalConfig(System.getenv().containsKey("DEBUG")));
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Debugging enabled.");
        }
        if (args == null || args.length < 1) {
            System.err.println("Error: invalid number of args.");
            printHelp();
            System.exit(1);
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Args: " + Arrays.toString(args));
        }

        boolean dryRun = false;
        boolean skipCheck = false;
        final String rawPath = args[args.length - 1];

        final Iterator<String> argsIterator = Arrays.asList(args).iterator();
        GlobalResultReport.ReportOutputType reportOutputType = GlobalResultReport.ReportOutputType.CONSOLE;
        while (argsIterator.hasNext()) {
            final String arg = argsIterator.next();
            switch (arg) {
                case "--dry-run":
                    dryRun = true;
                    break;
                case "--skip-check":
                    skipCheck = true;
                    break;
                case "--help":
                    printHelp();
                    return;
                case "--report":
                    final String outType = argsIterator.next();
                    switch (outType.toLowerCase()) {
                        case "json":
                            reportOutputType = GlobalResultReport.ReportOutputType.JSON;
                            break;
                        case "console":
                            reportOutputType = GlobalResultReport.ReportOutputType.CONSOLE;
                            break;
                        case "none":
                            reportOutputType = GlobalResultReport.ReportOutputType.NONE;
                            break;
                        default:
                            System.err.println("Unknown output type: " + outType);
                            printHelp();
                            System.exit(1);
                            return;
                    }
                    break;
                default:
                    if (arg.equals(rawPath)) break;
                    System.err.println("Unknown argument: " + arg);
                    printHelp();
                    System.exit(1);
                    return;
            }
        }

        final File start = new File(rawPath);
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Starting at: " + start);
        }

        Integer exitAfter = null;
        try {
            run(resolve(read(start), start), skipCheck, dryRun);
        } catch (ExitCodeException e) {
            exitAfter = e.code;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mReport.report(reportOutputType, System.out);
            if (exitAfter != null) System.exit(exitAfter);
        }
    }

    @Nonnull
    private static List<File> read(@Nonnull File start) throws Throwable {
        // Global stage: Reading
        if (GlobalConfig.get().DEBUG) {
            System.out.println("= GLOBAL STAGE: READING =");
        }
        return Files.walk(start.toPath())
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(file -> file.getName().toLowerCase().matches("\\d\\d-.*\\.ini"))
                .collect(Collectors.toList());
    }

    private static List<ConfItem> resolve(@Nonnull List<File> ini, @Nonnull File start) throws Throwable {
        // Global stage: Resolving
        if (GlobalConfig.get().DEBUG) {
            System.out.println();
            System.out.println("= GLOBAL STAGE: RESOLVING =");
        }
        return ini.stream()
                // Sort here so the execution order is kept in global report.
                .sorted((o1, o2) -> {
                    final String shortPath1 = o1.getAbsolutePath()
                            .substring(start.getAbsolutePath().length() + 1 /* Remove leading / */);
                    final String shortPath2 = o2.getAbsolutePath()
                            .substring(start.getAbsolutePath().length() + 1 /* Remove leading / */);
                    return shortPath1.compareTo(shortPath2);
                })
                .map(file -> {
                    return mReport.enterStage(file, GlobalResultReport.ItemStageType.RESOLVE, () -> {
                        try {
                            if (GlobalConfig.get().DEBUG) {
                                System.out.println();
                                System.out.println("Resolving " + file);
                            }
                            final ConfItemEnvironment env = ConfItemEnvironment.resolve(file, start);
                            final ConfItem item = new ConfItem(env);
                            return new GlobalResultReport.ResultPair<>(GlobalResultReport.StageStatus.SUCCESS, item);
                        } catch (IOException | IllegalStateException e) {
                            System.err.println("Cannot resolve " + file.getAbsolutePath() + ": " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    });
                })
                .collect(Collectors.toList());
    }

    private static void run(@Nonnull List<ConfItem> tasks, boolean skipCheck, boolean dryRun) throws Throwable {
        // Global stage: Running
        if (GlobalConfig.get().DEBUG) {
            System.out.println();
            System.out.println("= GLOBAL STAGE: RUNNING =");
        }
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
            final File ini = item.getEnv().arguments().getFile();
            int r;
            // Conditions
            r = mReport.enterStage(ini, GlobalResultReport.ItemStageType.VERIFY, () -> {
                if (GlobalConfig.get().DEBUG) {
                    System.out.println("Verifying conditions...");
                }
                final int res = item.verify();
                return new GlobalResultReport.ResultPair<>(res == 0 ? GlobalResultReport.StageStatus.SUCCESS :
                        GlobalResultReport.StageStatus.FAILURE, res);
            });
            if (r != 0) {
                System.err.println("Failed verifying " + item.toString());
                // Skip the following.
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.CHECK, GlobalResultReport.StageStatus.SKIPPED);
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.PRE_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.RUN, GlobalResultReport.StageStatus.SKIPPED);
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.POST_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                // Stop here.
                break;
            }

            // Check
            if (skipCheck) {
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.CHECK, GlobalResultReport.StageStatus.SKIPPED);
            } else {
                r = mReport.enterStage(ini, GlobalResultReport.ItemStageType.CHECK, () -> {
                    if (GlobalConfig.get().DEBUG) {
                        System.out.println("Checking...");
                    }
                    final int res = item.check();
                    return new GlobalResultReport.ResultPair<>(res == 0 ? GlobalResultReport.StageStatus.SUCCESS :
                            GlobalResultReport.StageStatus.FAILURE, res);
                });
                if (r != 0) {
                    System.err.println("Failed checking " + item.toString());
                    // Skip the following.
                    mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.PRE_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                    mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.RUN, GlobalResultReport.StageStatus.SKIPPED);
                    mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.POST_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                    // Stop the whole process
                    throw new ExitCodeException(r);
                }
            }

            if (dryRun) {
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.PRE_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.RUN, GlobalResultReport.StageStatus.SKIPPED);
                mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.POST_HOOK, GlobalResultReport.StageStatus.SKIPPED);
            } else {
                // Pre-hook
                r = mReport.enterStage(ini, GlobalResultReport.ItemStageType.PRE_HOOK, () -> {
                    if (GlobalConfig.get().DEBUG) {
                        System.out.println("Running pre-exec hook...");
                    }
                    final int res = item.before();
                    return new GlobalResultReport.ResultPair<>(res == 0 ? GlobalResultReport.StageStatus.SUCCESS :
                            GlobalResultReport.StageStatus.FAILURE, res);
                });
                if (r != 0) {
                    System.err.println("Failed running pre-exec hook of " + item.toString());
                    // Skip the following.
                    mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.RUN, GlobalResultReport.StageStatus.SKIPPED);
                    mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.POST_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                    // Stop the whole process
                    throw new ExitCodeException(r);
                }

                // Run
                r = mReport.enterStage(ini, GlobalResultReport.ItemStageType.RUN, () -> {
                    if (GlobalConfig.get().DEBUG) {
                        System.out.println("Running...");
                    }
                    final int res = item.run();
                    return new GlobalResultReport.ResultPair<>(res == 0 ? GlobalResultReport.StageStatus.SUCCESS :
                            GlobalResultReport.StageStatus.FAILURE, res);
                });
                if (r != 0) {
                    System.err.println("Failed to run " + item.toString());
                    // Skip the following.
                    mReport.setStageStatus(ini, GlobalResultReport.ItemStageType.POST_HOOK, GlobalResultReport.StageStatus.SKIPPED);
                    // Stop the whole process
                    throw new ExitCodeException(r);
                }


                // Post-hook
                r = mReport.enterStage(ini, GlobalResultReport.ItemStageType.PRE_HOOK, () -> {
                    if (GlobalConfig.get().DEBUG) {
                        System.out.println("Running post-exec hook...");
                    }
                    final int res = item.after();
                    return new GlobalResultReport.ResultPair<>(res == 0 ? GlobalResultReport.StageStatus.SUCCESS :
                            GlobalResultReport.StageStatus.FAILURE, res);
                });
                if (r != 0) {
                    System.err.println("Failed running post-exec hook of " + item.toString());
                    // Skip the following.
                    // Stop the whole process
                    throw new ExitCodeException(r);
                }
            }

            if (GlobalConfig.get().DEBUG) {
                System.out.println("Done.");
            }
        }
    }

    private static void printHelp() {
        System.out.println("Usage: confman [Options] <path to your config package>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --dry-run: Only perform verify and check");
        System.out.println("  --skip-check: Skip checking [Dangerous!]");
        System.out.println("  --help: Show this help");
        System.out.println("  --report: Final report type (possible values: console, json, none)");
    }

    private static class ExitCodeException extends Exception {
        private final int code;

        public ExitCodeException(int code) {
            this.code = code;
        }
    }
}
