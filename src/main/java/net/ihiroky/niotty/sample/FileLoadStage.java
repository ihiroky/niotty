package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.buffer.Buffers;

import java.io.File;
import java.io.IOException;

/**
 * @author Hiroki Itoh
 */
public class FileLoadStage extends LoadStage {

    private static final String FILE = "build.gradle";

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
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
            Packet first = Buffers.newPacket(path, 0, fileSize);
            context.transport().write(first);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        context.transport().close();
    }
}
