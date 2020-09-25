package segfault.confman.confpkg;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.runners.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * An item of the configuration package.
 */
public final class ConfItem extends TaskRunner {
    protected final HookRunner mHookRunner;
    private final TaskRunner mRunner;

    public ConfItem(@Nonnull ConfItemEnvironment env,
                    @Nonnull HookRunner hookRunner,
                    @Nonnull TaskRunner runner) {
        super(env);
        this.mHookRunner = hookRunner;
        this.mRunner = runner;
    }

    public ConfItem(@Nonnull ConfItemEnvironment env) throws IllegalStateException {
        this(env,
                new HookRunner(env),
                env.resolveRunner());
    }

    @Override
    public final int check() {
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Performing check");
        }
        int r;
        r = mHookRunner.check();
        if (r != 0) {
            System.err.println("Check function in hook returns a failure: " + r);
            return r;
        }
        r = mRunner.check();
        if (r != 0) {
            System.err.println("The task runner returns a failure when checking: " + r);
            return r;
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Check passed");
        }
        return r;
    }

    @Override
    public final int before() {
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Performing pre-execution hook");
        }
        int r;
        r = mHookRunner.before();
        if (r != 0) {
            System.err.println("Before function in hook returns a failure: " + r);
            return r;
        }
        r = mRunner.before();
        if (r != 0) {
            System.err.println("The task runner returns a failure when running before hook: " + r);
            return r;
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Pre-execution hook is executed successfully");
        }
        return r;
    }

    @Override
    public final int run() {
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Running the task");
        }
        int r;
        // The hook cannot run the task.
        r = mRunner.run();
        if (r != 0) {
            System.err.println("The task runner returns a failure when running the task: " + r);
            return r;
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("The task is executed successfully");
        }
        return r;
    }

    @Override
    public final int after() {
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Performing post-execution hook");
        }
        int r;
        r = mHookRunner.after();
        if (r != 0) {
            System.err.println("After function in hook returns a failure: " + r);
            return r;
        }
        r = mRunner.after();
        if (r != 0) {
            System.err.println("The task runner returns a failure when running after hook: " + r);
            return r;
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("Post-execution hook is executed successfully");
        }
        return r;
    }

    @Override
    public String toString() {
        return mEnv.arguments().getFile().getAbsolutePath()
                .substring(mEnv.startingPoint().getAbsolutePath().length() + 1 /* Remove leading / */);
    }
}
