package segfault.confman.confpkg.runners;

import segfault.confman.GlobalConfig;
import segfault.confman.confpkg.ConfItemEnvironment;
import segfault.confman.confpkg.FileUtils;
import segfault.confman.confpkg.TaskRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeleteItemRunner extends TaskRunner {
    public DeleteItemRunner(@Nonnull ConfItemEnvironment env) {
        super(env);
    }

    @Override
    public int check() {
        final File target = mEnv.toSystemFile();
        if (!target.exists()) {
            if (!Boolean.parseBoolean(mEnv.getArgument("Delete", "ContinueWhenNotExist", "false"))) {
                System.err.println("Cannot find " + target + " on the local system.");
                return -1;
            }
        }
        if (!target.getParentFile().canWrite()) {
            System.err.println("Deleting " + target + " is not permitted. Consult your administrator.");
            return -2;
        }
        final List<File> deniedFiles = new ArrayList<>();
        try {
            Files.walkFileTree(target.toPath(), new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    if (GlobalConfig.get().DEBUG) {
                        System.out.println("Walking " + path);
                    }
                    if (!path.toFile().getParentFile().canWrite()) {
                        deniedFiles.add(path.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("Cannot walk through " + target + ": " + e.getMessage());
        }
        if (!deniedFiles.isEmpty()) {
            deniedFiles.forEach(file -> System.err.println(file + " does not have permission to delete."));
            return -2;
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
        if (target.exists()) {
            return 0;
        }
        if (!FileUtils.deleteFolder(target)) {
            System.err.println("Cannot delete " + target);
            return -1;
        } else {
            if (GlobalConfig.get().DEBUG) {
                System.out.println("Deleted " + target);
            }
            return 0;
        }
    }

    @Override
    public int after() {
        return 0;
    }
}
