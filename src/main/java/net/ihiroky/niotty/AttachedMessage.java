package net.ihiroky.niotty;

/**
 * An message that an object is attached to.
 *
 * @param <I> the type of the message.
 * @author Hiroki Itoh
 */
public class AttachedMessage<I> {

    private final I message_;
    private final Object attachment_;

    public AttachedMessage(I message) {
        message_ = message;
        attachment_ = null;
    }

    public AttachedMessage(I message, Object attachment) {
        message_ = message;
        attachment_ = attachment;
    }

    public I message() {
        return message_;
    }

    public Object attachment() {
        return attachment_;
    }

    <O> StageContext<O> wrappedContext(PipelineElement<?, O> pipelineElement) {
        return new WrappedStageContext<>(pipelineElement, attachment_);
    }

    private static class WrappedStageContext<O> implements StageContext<O> {

        private final PipelineElement<?, O> context_;
        private final Object attachment_;

        WrappedStageContext(PipelineElement<?, O> context, Object attachment) {
            context_ = context;
            attachment_ = attachment;
        }

        @Override
        public StageKey key() {
            return context_.key();
        }

        @Override
        public Transport transport() {
            return context_.transport();
        }

        @Override
        public Object attachment() {
            return attachment_;
        }

        @Override
        public void proceed(O output) {
            AttachedMessage<O> am = new AttachedMessage<>(output, attachment_);
            context_.proceed(am);
        }

        @Override
        public void proceed(TransportStateEvent event) {
            context_.proceed(event);
        }
    }
}
