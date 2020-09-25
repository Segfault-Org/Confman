package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ExecRunner extends AbsCommandRunner {
    public ExecRunner(@Nonnull ConfItemEnvironment env) {
        super(env,
                null /* checkCommand */,
                null /* beforeCommand */,
                patchCmd(env.item().getAbsolutePath(), env) /* runCommand */,
                null /* afterCommand */);
    }

    @Override
    public int check() {
        int r;
        r = super.check();
        if (r != 0) return r;
        if (!mEnv.item().canRead() ||
        !mEnv.item().canExecute()) {
            System.err.println(mEnv.item() + " does not have rx permission.");
            return -1;
        } else {
            if (GlobalConfig.get().DEBUG) {
                System.out.println(mEnv.item() + " has rx permission.");
            }
            return 0;
        }
    }

    @Nonnull
    private static String patchCmd(@Nonnull String cmd, @Nonnull ConfItemEnvironment env) {
        return cmd + " " + env.item().getAbsolutePath();
    }
}
