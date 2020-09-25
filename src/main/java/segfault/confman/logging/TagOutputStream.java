package segfault.confman.logging;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class TagOutputStream extends OutputStream {
    private final OutputStream mOut;
    private final String mTag;

    private boolean mPrintedTagForThisLine = false;

    public TagOutputStream(@Nonnull OutputStream out,
                           @Nonnull String tag) {
        mOut = out;
        mTag = tag;
    }

    private void writeTag() throws IOException {
        mOut.write(mTag.getBytes(Charset.defaultCharset()));
    }

    @Override
    public void write(int b) throws IOException {
        if (!mPrintedTagForThisLine) {
            writeTag();
            mPrintedTagForThisLine = true;
        }
        mOut.write(b);
        if (b == '\n') {
            mPrintedTagForThisLine = false;
        }
    }
}
