package segfault.confman.confpkg;

import javax.annotation.Nonnull;
import java.io.File;

public class FileUtils {
    public static boolean deleteFolder(@Nonnull File folder) {
        boolean result = true;
        final File[] files = folder.listFiles();
        if (files != null) {
            for (File f: files) {
                if (f.isDirectory()) {
                    if (!deleteFolder(f))
                        result = false;
                } else {
                    if (!f.delete()) {
                        System.err.println("Cannot delete " + f);
                        result = false;
                    }
                }
            }
        }
        if (!folder.delete()) {
            System.err.println("Cannot delete " + folder);
            result = false;
        }
        return result;
    }
}
