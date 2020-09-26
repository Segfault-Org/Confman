package segfault.confman.confpkg;

import segfault.confman.GlobalConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;

public class ExternalProcess {
    public static int exec(@Nonnull String[] commands, @Nullable Map<String, String> env, @Nullable File dir) {
        final String debugCmd = Arrays.toString(commands);
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Executing: \"" + debugCmd + "\" in " + dir);
        }

        final ProcessBuilder builder = new ProcessBuilder(commands)
                .directory(dir)
                .inheritIO();
        if (env != null) builder.environment().putAll(env);
        try {
            final int exit = builder.start().waitFor();
            if (exit != 0) {
                System.err.format("Failed to run command %1$s: exit code is %2$s\n", debugCmd, exit);
            } else {
                if (GlobalConfig.get().DEBUG) {
                    System.out.format("Command exits 0.\n");
                }
            }
            return exit;
        } catch (IOException | InterruptedException e) {
            System.err.format("Failed to run command %1$s: %2$s\n", debugCmd, e.getMessage());
            return -1;
        }
    }

    public static int execShell(@Nonnull String command, @Nullable Map<String, String> env, @Nullable File dir) {
        return exec(wrapWithShell(command),
                env,
                dir);
    }

    @Nonnull
    public static String[] wrapWithShell(@Nonnull String command) {
        return new String[]{"sh", "-c", command};
    }

    @Deprecated
    public static int exec(@Nonnull String command, @Nullable Map<String, String> env, @Nullable File dir) {
        return exec(splitCommandBySpace(command), env, dir);
    }

    @Nonnull
    public static String[] splitCommandBySpace(@Nonnull String command) {
        final StringTokenizer st = new StringTokenizer(command);
        final String[] cmdarray = new String[st.countTokens()];

        for(int i = 0; st.hasMoreTokens(); ++i) {
            cmdarray[i] = st.nextToken();
        }
        return cmdarray;
    }
}
