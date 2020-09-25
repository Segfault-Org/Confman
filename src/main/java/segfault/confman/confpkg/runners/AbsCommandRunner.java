package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.ExternalProcess;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public abstract class AbsCommandRunner extends TaskRunner {
    private final String mVerifyCommand;
    private final String mCheckCommand;
    private final String mBeforeCommand;
    private final String mRunCommand;
    private final String mAfterCommand;

    public AbsCommandRunner(@Nonnull ConfItemEnvironment env,
                            @Nullable String checkCommand,
                            @Nullable String beforeCommand,
                            @Nullable String runCommand,
                            @Nullable String afterCommand) {
        this(env, null, checkCommand, beforeCommand, runCommand, afterCommand);
    }

    public AbsCommandRunner(@Nonnull ConfItemEnvironment env,
                            @Nullable String verifyCommand,
                            @Nullable String checkCommand,
                            @Nullable String beforeCommand,
                            @Nullable String runCommand,
                            @Nullable String afterCommand) {
        super(env);
        mVerifyCommand = verifyCommand;
        mCheckCommand = checkCommand;
        mBeforeCommand = beforeCommand;
        mRunCommand = runCommand;
        mAfterCommand = afterCommand;
    }

    @Override
    public int verify() {
        if (mVerifyCommand != null) return exec(mVerifyCommand);
        return super.verify();
    }

    @Override
    public int check() {
        if (mCheckCommand != null) return exec(mCheckCommand);
        return 0;
    }

    @Override
    public int before() {
        if (mBeforeCommand != null) return exec(mBeforeCommand);
        return 0;
    }

    @Override
    public int run() {
        if (mRunCommand != null) return exec(mRunCommand);
        return 0;
    }

    @Override
    public int after() {
        if (mAfterCommand != null) return exec(mAfterCommand);
        return 0;
    }

    private int exec(@Nonnull String command) {
        return ExternalProcess.exec(command, null, mEnv.parentDir());
    }
}
