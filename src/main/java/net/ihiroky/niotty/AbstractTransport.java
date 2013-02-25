package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.BufferSink;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Hiroki Itoh
 */
abstract public class AbstractTransport<L extends EventLoop<L>> implements Transport {

    private LoadPipeline loadPipeline;
    private StorePipeline storePipeline;
    private AtomicReference<Object> attachmentReference;
    private TransportListener transportListener;
    private L loop;

    static final TransportListener NULL_LISTENER = new NullListener();

    protected AbstractTransport() {
        attachmentReference = new AtomicReference<>();
        transportListener = NULL_LISTENER;
    }

    protected final void setLoadPipeline(LoadPipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipeline");
        loadPipeline = pipeline;
    }

    protected LoadPipeline getLoadPipeline() {
        return loadPipeline;
    }

    protected final void setStorePipeline(StorePipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipeline");
        storePipeline = pipeline;
    }

    protected StorePipeline getStorePipeline() {
        return storePipeline;
    }

    public final void setEventLoop(L loop) {
        Objects.requireNonNull(loop, "loop");
        this.loop = loop;
    }

    public final L getEventLoop() {
        return loop;
    }

    protected void offerTask(EventLoop.Task<L> task) {
        if (loop != null) {
            loop.offerTask(task);
        }
    }

    protected TransportListener getTransportListener() {
        return transportListener;
    }

    @Override
    public void addListener(TransportListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (this) {
            TransportListener oldListener = transportListener;
            if (oldListener == null) {
                transportListener = listener;
                return;
            }
            if (oldListener instanceof ListenerList) {
                ((ListenerList) oldListener).list.add(listener);
                return;
            }
            ListenerList listenerList = new ListenerList();
            listenerList.list.add(oldListener);
            listenerList.list.add(listener);
            transportListener = listenerList;
        }
    }

    @Override
    public void removeListener(TransportListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (this) {
            if (transportListener == listener) {
                transportListener = NULL_LISTENER;
                return;
            }
            if (transportListener instanceof ListenerList) {
                ListenerList listenerList = (ListenerList) transportListener;
                listenerList.list.remove(transportListener);
                if (listenerList.list.size() == 1) {
                    transportListener = listenerList.list.get(0);
                }
            }
        }
    }

    @Override
    public Object attach(Object attachment) {
        return attachmentReference.getAndSet(attachment);
    }

    @Override
    public Object attachment() {
        return attachmentReference.get();
    }


    abstract protected void writeDirect(BufferSink buffer);

    private static class NullListener implements TransportListener {
        @Override
        public void onBind(Transport transport, SocketAddress local) {
        }

        @Override
        public void onConnect(Transport transport, SocketAddress remote) {
        }

        @Override
        public void onJoin(Transport transport, InetAddress group, NetworkInterface ni, InetAddress source) {
        }

        @Override
        public void onClose(Transport transport) {
        }
    }

    private static class ListenerList implements TransportListener {

        CopyOnWriteArrayList<TransportListener> list = new CopyOnWriteArrayList<>();

        @Override
        public void onBind(Transport transport, SocketAddress local) {
            for (TransportListener listener : list) {
                listener.onBind(transport, local);
            }
        }

        @Override
        public void onConnect(Transport transport, SocketAddress remote) {
            for (TransportListener listener : list) {
                listener.onConnect(transport, remote);
            }
        }

        @Override
        public void onJoin(Transport transport, InetAddress group, NetworkInterface ni, InetAddress source) {
            for (TransportListener listener : list) {
                listener.onJoin(transport, group, ni, source);
            }
        }

        @Override
        public void onClose(Transport transport) {
            for (TransportListener listener : list) {
                listener.onClose(transport);
            }
        }
    }
}
