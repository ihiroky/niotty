package net.ihiroky.niotty.sample.file;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;

import java.io.File;
import java.io.IOException;

/**
 * @author Hiroki Itoh
 */
public class FileLoadStage implements LoadStage<String, Void> {

    @Override
    public void load(StageContext<Void> context, String input) {
        File path = new File(input);
        try {
            long fileSize = path.length();
            BufferSink first = Buffers.newBufferSink(path, 0, fileSize);
            context.transport().write(first);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void load(StageContext<Void> context, TransportStateEvent event) {
    }
}
