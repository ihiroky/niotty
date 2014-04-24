package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.util.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>Stops store operation if the number of pending write buffers in {@link net.ihiroky.niotty.Transport}
 * exceeds a limit.</p>
 * <p>This class has two thresholds: alert and limit. These are used for output control of the log.
 * If the number of buffers exceeds or falls below the alert, then the alert log is output.
 * If exceeds the limit, then the over limit log. Once the number of buffers exceeds the limit,
 * then the stored message is never proceeded to the next stage of this instance.</p>
 *
 * TODO test implementation
 */
public class StoreShutter extends StoreStage {

    private final int alert_;
    private final int limit_;
    private final long checkIntervalNanos_;
    private State state_;

    private static Logger logger_ = LoggerFactory.getLogger(StoreShutter.class);

    enum State {
        UNDER_ALERT, OVER_ALERT, OVER_LIMIT
    }

    /**
     * Constructs the instance.
     * @param alert the alert
     * @param limit the limit
     * @param checkIntervalSeconds interval (seconds) to check the pending write buffers
     */
    public StoreShutter(int alert, int limit, int checkIntervalSeconds) {
        alert_ = Arguments.requirePositive(alert, "alert");
        limit_ = Arguments.requirePositive(limit, "limit");
        checkIntervalNanos_ = TimeUnit.SECONDS.toNanos(
                Arguments.requirePositive(checkIntervalSeconds, "checkIntervalSeconds"));
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {
        if (state_ != State.OVER_LIMIT) {
            context.proceed(message, parameter);
        }
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(final StageContext context) {
        state_ = State.UNDER_ALERT;
        context.schedule(new Event() {
            @Override
            public long execute() throws Exception {
                Transport transport = context.transport();
                int pendingWriteBuffers = transport.pendingWriteBuffers();
                State state = state_;

                if (state == State.OVER_LIMIT) {
                    return Event.DONE;
                }

                if (pendingWriteBuffers > limit_) {
                    state_ = State.OVER_LIMIT;
                    logger_.error("The number of pending buffers {} for {}({}) exceeds the limit {}. Stop writing.",
                            pendingWriteBuffers, transport.remoteAddress(), transport.attachment(), limit_);
                    return Event.DONE;
                }

                if (pendingWriteBuffers > alert_ && state == State.UNDER_ALERT) {
                    logger_.error("The number of pending buffers {} for {}({}) exceeds the alert size {}.",
                            pendingWriteBuffers, transport.remoteAddress(), transport.attachment(), alert_);
                    state_ = State.OVER_ALERT;
                } else if (pendingWriteBuffers <= alert_ && state == State.OVER_ALERT) {
                    logger_.error("The number of pending buffers {} for {}({}) falls below the alert size {}.",
                            pendingWriteBuffers, transport.remoteAddress(), transport.attachment(), alert_);
                    state_ = State.UNDER_ALERT;
                }
                return checkIntervalNanos_;
            }
        }, checkIntervalNanos_, TimeUnit.NANOSECONDS);
    }

    @Override
    public void deactivated(StageContext context) {
        state_ = State.OVER_LIMIT;
    }

    @Override
    public void eventTriggered(StageContext context, Object event) {
    }

    State state() {
        return state_;
    }

    void setState(State state) {
        state_ = state;
    }
}
