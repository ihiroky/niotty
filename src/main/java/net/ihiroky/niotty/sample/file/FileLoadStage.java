package net.ihiroky.niotty.sample.file;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.FileBufferSink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Hiroki Itoh
 */
public class FileLoadStage implements LoadStage<String, Void> {

    private static final int DIVISOR = 2;

    @Override
    public void load(LoadStageContext<String, Void> context, String input) {
        Path path = Paths.get(input);
        try {
            long fileSize = Files.size(path);
            FileBufferSink first = Buffers.newBufferSink(path, 0, fileSize);
            context.transport().write(first);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void load(LoadStageContext<String, Void> context, TransportStateEvent event) {
    }
}
