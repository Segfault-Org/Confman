package segfault.confman.confpkg;

import javax.annotation.Nonnull;

public abstract class TaskRunner {
    protected final ConfItemEnvironment mEnv;

    public TaskRunner(@Nonnull ConfItemEnvironment env) {
        this.mEnv = env;
    }

    public int verify() {
        return 0;
    }
    public abstract int check();
    public abstract int before();
    public abstract int run();
    public abstract int after();

    @Nonnull
    public final ConfItemEnvironment getEnv() {
        return mEnv;
    }
}
