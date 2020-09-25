package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileLock;

public abstract class AbsWriteFileRunner extends TaskRunner {
    private final File mTarget;
    private final ExistencePolicy mExistencePolicy;

    public AbsWriteFileRunner(@Nonnull ConfItemEnvironment env,
                              @Nonnull File target,
                              @Nonnull ExistencePolicy existencePolicy) {
        super(env);
        mTarget = target;
        mExistencePolicy = existencePolicy;
    }

    @Override
    public int check() {
        if (mTarget.exists()) {
            if (mExistencePolicy == ExistencePolicy.FAIL_IF_EXIST) {
                System.err.println("The file " + mTarget + " already exists on the local system.");
                return -1;
            }
            if (!mTarget.isFile()) {
                System.err.println("The file " + mTarget + " exists but is a folder.");
                return -1;
            }
            if (!mTarget.canWrite()) {
                System.out.println("Cannot write to " + mTarget);
                return -1;
            }
        } else {
            if (mExistencePolicy == ExistencePolicy.FAIL_IF_NOT_EXIST) {
                System.err.println("The file " + mTarget + " does not exists on the local system.");
                return -1;
            }
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println(mTarget + " passed existence check.");
        }

        // Test if we can write to sysFile.
        File currentlyTest = mTarget;
        while (true) {
            if (!currentlyTest.exists()) {
                if (GlobalConfig.get().DEBUG) {
                    System.out.println(currentlyTest + " does not exist. We will mkdir later.");
                }
            } else {
                if (currentlyTest == mTarget) {
                    if (!currentlyTest.canWrite()) {
                        // This should never happen since we had already performed the permission test above.
                        throw new IllegalStateException("BUG");
                    }
                } else if (currentlyTest.isFile()) {
                    System.err.println(currentlyTest + " should be a folder (the parent folder of the target), but is a file.");
                    return -2;
                } else if (!currentlyTest.canWrite()) {
                    System.err.println(currentlyTest + " is a parent folder but we cannot write to it.");
                    return -2;
                } else {
                    // Stop here. We only need to test the closest existing parent folder.
                    if (GlobalConfig.get().DEBUG) {
                        System.out.println(currentlyTest + " passed all tests. Stopping loop.");
                    }
                    break;
                }
            }
            currentlyTest = currentlyTest.getParentFile();
            if (currentlyTest == null) {
                if (GlobalConfig.get().DEBUG) {
                    System.out.println("Reached the root.");
                }
                break;
            }
        }
        if (GlobalConfig.get().DEBUG) {
            System.out.println("All tests passed.");
        }
        return 0;
    }

    @Override
    public int before() {
        return 0;
    }

    @Override
    public int run() {
        FileOutputStream out = null;
        FileLock lock = null;
        try {
            if (!mTarget.getParentFile().exists()) {
                if (!mTarget.getParentFile().mkdirs()) {
                    System.err.println("Unable to mkdir " + mTarget.getParent());
                    return -4;
                }
            }
            out = new FileOutputStream(mEnv.toSystemFile());
            lock = out.getChannel().lock();
            writeTo(out);
        } catch (IOException e) {
            System.err.format("Cannot write to %1$s: %2$s", mEnv.toSystemFile(), e.getMessage());
            return -3;
        } finally {
            try {
                if (out != null) out.close();
                if (lock != null) lock.close();
            } catch (IOException ignored) {}
        }
        return 0;
    }

    @Override
    public int after() {
        return 0;
    }

    public abstract void writeTo(@Nonnull OutputStream out) throws IOException;

    public enum ExistencePolicy {
        FAIL_IF_EXIST,
        OVERRIDE_IF_EXIST,
        WRITE_IF_NOT_EXIST,
        FAIL_IF_NOT_EXIST
    }
}
