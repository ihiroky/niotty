package net.ihiroky.niotty.sample.file;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;

import java.io.File;
import java.io.IOException;

/**
 * @author Hiroki Itoh
 */
public class FileLoadStage extends LoadStage {

    private static final String FILE = "build.gradle";

    @Override
    public void loaded(StageContext context, Object message) {
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
        exception.printStackTrace();
    }

    @Override
    public void activated(StageContext context) {
        File path = new File(FILE);
        try {
            long fileSize = path.length();
            BufferSink first = Buffers.newBufferSink(path, 0, fileSize);
            context.transport().write(first);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void deactivated(StageContext context, Pipeline.DeactivateState state) {
        context.transport().close();
    }
}
