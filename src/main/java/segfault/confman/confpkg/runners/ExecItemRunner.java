package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;

import javax.annotation.Nonnull;

public class ExecItemRunner extends AbsCommandRunner {
    public ExecItemRunner(@Nonnull ConfItemEnvironment env) {
        super(env,
                null /* verifyCommand */,
                null /* checkCommand: custom check */,
                null /* beforeCommand: no need of before hook */,
                new String[]{ env.item().getAbsolutePath(),
                env.getArgument("Exec", "Args", null)},
                null /* afterCommand: no need of after hook */);
    }

    @Override
    public int check() {
        if (mEnv.item().canRead() &&
        mEnv.item().canExecute()) {
            if (GlobalConfig.get().DEBUG) {
                System.out.println(mEnv.item() + " has rx.");
            }
            return 0;
        } else {
            System.err.println(mEnv.item() + " does not have rx permission.");
            return -1;
        }
    }
}
