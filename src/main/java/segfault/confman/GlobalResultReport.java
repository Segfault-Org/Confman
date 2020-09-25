package segfault.confman;

import segfault.confman.logging.TagOutputStream;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

final class GlobalResultReport {
    private final Map<File /* INI */, Map<ItemStageType, StageStatus>> mResults;

    public GlobalResultReport() {
        mResults = new HashMap<>();
    }

    public GlobalResultReport(int size) {
        mResults = new HashMap<>(size);
    }

    private void createIniRecord(@Nonnull File ini) {
        final Map<ItemStageType, StageStatus> initial = new HashMap<>(6);
        initial.put(ItemStageType.RESOLVE, StageStatus.NOT_STARTED);
        initial.put(ItemStageType.VERIFY, StageStatus.NOT_STARTED);
        initial.put(ItemStageType.CHECK, StageStatus.NOT_STARTED);
        initial.put(ItemStageType.PRE_HOOK, StageStatus.NOT_STARTED);
        initial.put(ItemStageType.RUN, StageStatus.NOT_STARTED);
        initial.put(ItemStageType.POST_HOOK, StageStatus.NOT_STARTED);
        mResults.put(ini, initial);
    }

    public void setStageStatus(@Nonnull File ini, @Nonnull ItemStageType stage, @Nonnull StageStatus status) {
        if (!mResults.containsKey(ini))
            createIniRecord(ini);
        final Map<ItemStageType, StageStatus> record = mResults.get(ini);
        record.put(stage, status);
        mResults.put(ini, record);
    }

    public <T> T enterStage(@Nonnull File ini, @Nonnull ItemStageType stage, @Nonnull StageRunnable<T> runnable) {
        final PrintStream rawStdout = System.out;
        final PrintStream rawStderr = System.err;

        final String tag = String.format("[%1$s]: ", ini.getName());
        final PrintStream stageStdout = new PrintStream(new TagOutputStream(rawStdout, tag));
        final PrintStream stageStderr = new PrintStream(new TagOutputStream(rawStderr, tag));
        System.setOut(stageStdout);
        System.setErr(stageStderr);
        try {
            setStageStatus(ini, stage, StageStatus.RUNNING);
            final ResultPair<T> result = runnable.run();
            setStageStatus(ini, stage, result.status);
            return result.result;
        } catch (Throwable e) {
            System.err.println("Fetal exception: " + e.getMessage());
            setStageStatus(ini, stage, StageStatus.FAILURE);
            throw new RuntimeException(e);
        } finally {
            System.setOut(rawStdout);
            System.setErr(rawStderr);
            stageStdout.close();
            stageStderr.close();
        }
    }

    @FunctionalInterface
    public interface StageRunnable<T> {
        @Nonnull
        ResultPair<T> run();
    }

    public static class ResultPair<T> {
        public final StageStatus status;
        public final T result;

        public ResultPair(@Nonnull StageStatus status, T result) {
            this.status = status;
            this.result = result;
        }
    }

    public enum ItemStageType {
        RESOLVE,
        VERIFY,
        CHECK,
        PRE_HOOK,
        RUN,
        POST_HOOK
    }

    public enum StageStatus {
        NOT_STARTED,
        SKIPPED,
        RUNNING,
        SUCCESS,
        FAILURE
    }
}
