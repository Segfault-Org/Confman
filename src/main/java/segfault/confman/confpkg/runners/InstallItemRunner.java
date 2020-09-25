package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.channels.FileLock;

public class InstallItemRunner extends AbsWriteFileRunner {
    public InstallItemRunner(@Nonnull ConfItemEnvironment env) {
        super(env,
                env.toSystemFile(),
                Boolean.parseBoolean(env.getArgument("Install", "Force", "false")) ?
                ExistencePolicy.FAIL_IF_EXIST : ExistencePolicy.OVERRIDE_IF_EXIST);
    }

    @Override
    public void writeTo(@Nonnull OutputStream out) throws IOException {
        final InputStream in = new FileInputStream(mEnv.item());
        out.write(in.readAllBytes());
        in.close();
    }
}
