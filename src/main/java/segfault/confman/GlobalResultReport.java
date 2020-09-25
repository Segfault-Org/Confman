package segfault.confman;

import segfault.confman.logging.TagOutputStream;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class GlobalResultReport {
    private final Map<File /* INI */, Map<ItemStageType, StageStatus>> mResults;

    public GlobalResultReport() {
        mResults = new LinkedHashMap<>();
    }

    public GlobalResultReport(int size) {
        mResults = new LinkedHashMap<>(size);
    }

    private void createIniRecord(@Nonnull File ini) {
        final Map<ItemStageType, StageStatus> initial = new LinkedHashMap<>(6);
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

    public void report(@Nonnull ReportOutputType type,
                       @Nonnull Appendable appendable) throws IOException {
        if (type == ReportOutputType.NONE)
            return;
        if (GlobalConfig.get().DEBUG) {
            System.out.println();
            System.out.println("= Generating Report =");
        }
        final Set<File> inis = mResults.keySet();
        switch (type) {
            case CONSOLE:
                for (final File ini : inis) {
                    appendable.append(ini.getAbsolutePath());
                    appendable.append('\n');
                    final Map<ItemStageType, StageStatus> stages = mResults.get(ini);
                    for (final ItemStageType stage : stages.keySet()) {
                        appendable.append("  ");
                        appendable.append(stage.toString());
                        appendable.append("\t");
                        appendable.append(stages.get(stage).toString());
                        appendable.append("\n");
                    }
                    appendable.append("\n");
                }
                break;
            case JSON:
                appendable.append("[\n");
                final Iterator<File> iniIterator = inis.iterator();
                while(iniIterator.hasNext()) {
                    final File ini = iniIterator.next();
                    appendable.append("  {\n");
                    appendable.append("    \"ini\": \"");
                    appendable.append(ini.getAbsolutePath());
                    appendable.append("\",\n");
                    appendable.append("    \"stages\": [\n");
                    final Map<ItemStageType, StageStatus> stages = mResults.get(ini);
                    final Iterator<ItemStageType> stagesIterator = stages.keySet().iterator();
                    while (stagesIterator.hasNext()) {
                        final ItemStageType stage = stagesIterator.next();
                        appendable.append("      {\n");
                        appendable.append("        \"stage\": \"");
                        appendable.append(stage.toString());
                        appendable.append("\",\n");
                        appendable.append("        \"result\": \"");
                        appendable.append(stages.get(stage).toString());
                        appendable.append("\"\n");
                        appendable.append("      }");
                        if (stagesIterator.hasNext())
                            appendable.append(",\n");
                        else
                            appendable.append("\n");
                    }
                    appendable.append("    ]\n");
                    appendable.append("  }");
                    if (iniIterator.hasNext())
                        appendable.append(",\n");
                    else
                        appendable.append("\n");
                }
                appendable.append("]\n");
                break;
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

    public enum ReportOutputType {
        CONSOLE,
        JSON,
        NONE
    }
}
