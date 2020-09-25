package segfault.confman;

import javax.annotation.Nonnull;

public class GlobalConfig {
    private static GlobalConfig sInstance;

    @Nonnull
    public static GlobalConfig get() {
        if (sInstance == null)
            throw new IllegalStateException("Assert fail: The GlobalConfig is not initialized.");
        return sInstance;
    }

    static void set(@Nonnull GlobalConfig config) {
        if (sInstance != null)
            throw new IllegalStateException("Assert fail: The GlobalConfig is already initialized.");
        sInstance = config;
    }

    public final boolean DEBUG;

    GlobalConfig(boolean debug) {
        DEBUG = debug;
    }
}
