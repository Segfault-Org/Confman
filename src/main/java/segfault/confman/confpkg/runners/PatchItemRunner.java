package segfault.confman.confpkg.runners;

import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.ExternalProcess;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.File;

public class PatchItemRunner extends TaskRunner {
    public PatchItemRunner(@Nonnull ConfItemEnvironment env) {
        super(env);
    }

    @Override
    public int check() {
        final File target = mEnv.toSystemFile();
        return ExternalProcess.exec(new String[]{"patch",
                "--dry-run",
                "--read-only=fail",
                target.getAbsolutePath(),
                mEnv.item().getAbsolutePath()}, null, mEnv.parentDir());
    }

    @Override
    public int before() {
        return 0;
    }

    @Override
    public int run() {
        final File target = mEnv.toSystemFile();
        return ExternalProcess.exec(new String[]{"patch",
                "--read-only=fail",
                target.getAbsolutePath(),
                mEnv.item().getAbsolutePath()}, null, mEnv.parentDir());
    }

    @Override
    public int after() {
        return 0;
    }
}
