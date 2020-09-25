package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.File;

public class MkdirItemRunner extends TaskRunner {
    public MkdirItemRunner(@Nonnull ConfItemEnvironment env) {
        super(env);
    }

    @Override
    public int check() {
        final File target = mEnv.toSystemFile();
        if (target.exists()) {
            if (!Boolean.parseBoolean(mEnv.getArgument("Mkdir", "Force", "false"))) {
                System.err.println(target + " exists on the local system.");
                return -1;
            } else {
                return 0;
            }
        }
        File test = target;
        while (true) {
            if (!test.exists()) {
                // Does not exist. We will mkdir later.
                if (GlobalConfig.get().DEBUG) {
                    System.out.println(test + " does not exist. We will mkdir later.");
                }
            } else {
                if (test.isFile()) {
                    System.err.println(test + " is a file.");
                    return -2;
                }
                if (!test.canWrite()) {
                    System.err.println("Cannot write to " + test);
                    return -2;
                }
                // Passed. We just need to check the closest exist parent.
                break;
            }
            test = test.getParentFile();
            if (GlobalConfig.get().DEBUG) {
                System.out.println("Parent " + test);
            }
            if (test == null) {
                if (GlobalConfig.get().DEBUG) {
                    System.out.println("Reached root.");
                }
                break;
            }
        }
        return 0;
    }

    @Override
    public int before() {
        return 0;
    }

    @Override
    public int run() {
        final File target = mEnv.toSystemFile();
        if (target.exists()) return 0;
        if (!target.mkdirs()) {
            System.err.println("Cannot mkdir.");
            return 1;
        } else {
            if (GlobalConfig.get().DEBUG) {
                System.out.println("mkdir done");
            }
            return 0;
        }
    }

    @Override
    public int after() {
        return 0;
    }
}
