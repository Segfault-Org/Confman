package segfault.confman.confpkg;

import com.google.auto.value.AutoValue;
import org.ini4j.Ini;
import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.runners.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@AutoValue
public abstract class ConfItemEnvironment {
    public static ConfItemEnvironment resolve(@Nonnull File iniFile, @Nonnull File startingPoint) throws IllegalStateException, IOException {
        // INI
        final Ini ini = new Ini(iniFile);

        // Item file
        final File finalItemFile;

        final File naturalItemFile = new File(iniFile.getParentFile(),
                iniFile.getName().substring(0, iniFile.getName().length() - 4));
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Natural item: " + naturalItemFile);
        }
        final String overrideItemPath = ini.get("Item", "Item");
        if (overrideItemPath != null) {
            finalItemFile = new File(iniFile.getParentFile(), overrideItemPath);
            if (GlobalConfig.get().DEBUG) {
                System.out.println("Using override item file");
            }
        } else {
            if (GlobalConfig.get().DEBUG) {
                System.out.println("Using natural item file");
            }
            finalItemFile = naturalItemFile;
        }

        if (!finalItemFile.exists() ||
                !finalItemFile.canRead()) {
            throw new IllegalStateException("Cannot read item file " + finalItemFile);
        }

        // Runner
        final String action = ini.get("Item", "Action");
        if (action == null) {
            throw new IllegalStateException("Action argument cannot be empty");
        }

        final ConfItemEnvironment env = new AutoValue_ConfItemEnvironment(
                finalItemFile,
                finalItemFile.getParentFile(),
                startingPoint,
                ini,
                action
        );
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Environment: " + env);
        }
        return env;
    }

    @Nonnull
    public abstract File item();

    @Nonnull
    public abstract File parentDir();

    @Nonnull
    public abstract File startingPoint();

    @Nonnull
    public abstract Ini arguments();

    @Nonnull
    public abstract String action();

    @Nonnull
    public final Path toSystemPath() {
        final String nameWithoutPrefix = item().getName().matches("\\d\\d-.*") ?
                item().getName().substring(3) :
                item().getName();
        final String path = "/" + new File(item().getParentFile(), nameWithoutPrefix)
                .getAbsolutePath().substring(startingPoint().getAbsolutePath().length());
        return Path.of(path);
    }

    @Nonnull
    public final File toSystemFile() {
        return toSystemPath().toFile();
    }

    public final String getArgument(@Nullable String section, @Nonnull String key, String defaultValue) {
        final Object value = arguments().get(section, key);
        if (value == null) return defaultValue;
        return value.toString();
    }

    @Nonnull
    public final TaskRunner resolveRunner() throws IllegalStateException {
        final TaskRunner runner;
        switch (action()) {
            case "delete":
                runner = new DeleteItemRunner(this);
                break;
            case "exec":
                runner = new ExecItemRunner(this);
                break;
            case "install":
                runner = new InstallItemRunner(this);
                break;
            case "mkdir":
                runner = new MkdirItemRunner(this);
                break;
            case "patch":
                runner = new PatchItemRunner(this);
                break;
            default:
                // TODO: External runner
                throw new IllegalStateException("Unknown action " + action());
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Matched runner: " + runner.getClass().getName());
        }
        return runner;
    }
}
