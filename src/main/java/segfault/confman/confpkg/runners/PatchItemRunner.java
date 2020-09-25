package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.ExternalProcess;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class PatchItemRunner extends TaskRunner {
    public PatchItemRunner(@Nonnull ConfItemEnvironment env) {
        super(env);
    }

    @Override
    public int check() {
        final File target = mEnv.toSystemFile();
        final String dryRunCmd = String.format("patch --dry-run --read-only=fail %1$s %2$s", target, mEnv.item().getAbsolutePath());
        return ExternalProcess.exec(dryRunCmd, null, mEnv.parentDir());
    }

    @Override
    public int before() {
        return 0;
    }

    @Override
    public int run() {
        final File target = mEnv.toSystemFile();
        final String dryRunCmd = String.format("patch --read-only=fail %1$s %2$s", target, mEnv.item().getAbsolutePath());
        return ExternalProcess.exec(dryRunCmd, null, mEnv.parentDir());
    }

    @Override
    public int after() {
        return 0;
    }
}
