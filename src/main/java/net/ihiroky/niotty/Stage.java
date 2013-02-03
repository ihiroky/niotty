package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/09, 17:23
 *
 * @author Hiroki Itoh
 */
public interface Stage<I, O> {

    void process(StageContext<I, O> context, MessageEvent<I> event);
    void process(StageContext<I, O> context, TransportStateEvent event);

    Stage<Object, Object> NULL = new Stage<Object, Object>() {
        @Override
        public void process(StageContext<Object, Object> context, MessageEvent<Object> event) {
        }
        @Override
        public void process(StageContext<Object, Object> context, TransportStateEvent event) {
        }
        @Override
        public String toString() {
            return "NULL_STAGE";
        }
    };

}
