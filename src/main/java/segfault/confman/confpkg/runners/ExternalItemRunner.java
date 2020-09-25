package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.ExternalProcess;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExternalItemRunner extends TaskRunner {
    private final File mRunnerExec;
    private final String mVerb;

    public ExternalItemRunner(@Nonnull ConfItemEnvironment env) throws IllegalStateException {
        super(env);
        mRunnerExec = new File(env.parentDir(), env.action());
        mVerb = env.getArgument("Item", "ActionVerb", null);
        if (GlobalConfig.get().DEBUG) {
            System.out.println("External runner: " + mRunnerExec);
            System.out.println("Runner verb: " + mVerb);
        }
    }

    @Override
    public int check() {
        return exec(Stage.CHECK);
    }

    @Override
    public int before() {
        return exec(Stage.PRE_HOOK);
    }

    @Override
    public int run() {
        return exec(Stage.RUN);
    }

    @Override
    public int after() {
        return exec(Stage.POST_HOOK);
    }

    private int exec(@Nonnull Stage stage) {
        final Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CONFMAN_EXT_INI", mEnv.arguments().getFile().getAbsolutePath());
        if (mVerb != null) env.put("CONFMAN_EXT_ACTION", mVerb);
        env.put("CONFMAN_EXT_STAGE", stage.toString());
        return ExternalProcess.exec(new String[]{mRunnerExec.getAbsolutePath()},
                env,
                mEnv.parentDir());
    }

    private enum Stage {
        CHECK("CHECK"),
        PRE_HOOK("BEFORE"),
        RUN("RUN"),
        POST_HOOK("AFTER")
        ;

        private final String value;

        Stage(@Nonnull String value) {
            this.value = value;
        }


        @Override
        public String toString() {
            return value;
        }
    }
}
