package segfault.confman.confpkg.runners;

import segfault.confman.confpkg.ConfItemEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HookRunner extends AbsCommandRunner {
    public HookRunner(@Nonnull ConfItemEnvironment env) {
        super(env,
                patchCmd(env.getArgument("Item", "ExecVerify", null), env),
                patchCmd(env.getArgument("Item", "ExecCheck", null), env),
                patchCmd(env.getArgument("Item", "ExecBefore", null), env),
                null /* runCommand */,
                patchCmd(env.getArgument("Item", "ExecAfter", null), env));
    }

    @Override
    public int run() {
        throw new UnsupportedOperationException("Hook cannot run directly.");
    }

    @Nullable
    private static String patchCmd(@Nullable String cmd, @Nonnull ConfItemEnvironment env) {
        if (cmd == null) return null;
        return cmd.replace("%file", env.item().getAbsolutePath());
    }
}
