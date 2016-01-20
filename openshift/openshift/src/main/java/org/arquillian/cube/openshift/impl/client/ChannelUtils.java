package org.arquillian.cube.openshift.impl.client;

import static org.xnio._private.Messages.msg;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.xnio.ChannelExceptionHandler;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

public final class ChannelUtils {

    public static <I extends StreamSourceChannel, O extends StreamSinkChannel> void initiateTransfer(long count, final I source, final O sink, Pool<ByteBuffer> pool) {
        ChannelUtils.initiateTransfer(
                count,
                source,
                sink,
                ChannelListeners.closingChannelListener(),
                ChannelListeners.<StreamSinkChannel> writeShutdownChannelListener(
                        ChannelListeners.closingChannelListener(),
                        ChannelListeners.closingChannelExceptionHandler()),
                ChannelListeners.closingChannelExceptionHandler(),
                ChannelListeners.closingChannelExceptionHandler(),
                pool);
    }

    /**
     * This is basically a copy of ChannelListeners.initiateTransfer(), but invokes a flush() on the sink after writing is complete.
     * 
     * Initiate a low-copy transfer between two stream channels.  The pool should be a direct buffer pool for best
     * performance.
     *
     * @param count the number of bytes to transfer, or {@link Long#MAX_VALUE} to transfer all remaining bytes
     * @param source the source channel
     * @param sink the target channel
     * @param sourceListener the source listener to set and call when the transfer is complete, or {@code null} to clear the listener at that time
     * @param sinkListener the target listener to set and call when the transfer is complete, or {@code null} to clear the listener at that time
     * @param readExceptionHandler the read exception handler to call if an error occurs during a read operation
     * @param writeExceptionHandler the write exception handler to call if an error occurs during a write operation
     * @param pool the pool from which the transfer buffer should be allocated
     */
    public static <I extends StreamSourceChannel, O extends StreamSinkChannel> void initiateTransfer(long count, final I source, final O sink, final ChannelListener<? super I> sourceListener, final ChannelListener<? super O> sinkListener, final ChannelExceptionHandler<? super I> readExceptionHandler, final ChannelExceptionHandler<? super O> writeExceptionHandler, Pool<ByteBuffer> pool) {
        if (pool == null) {
            throw msg.nullParameter("pool");
        }
        final Pooled<ByteBuffer> allocated = pool.allocate();
        boolean free = true;
        try {
            final ByteBuffer buffer = allocated.getResource();
            long transferred;
            for(;;) {
                try {
                    transferred = source.transferTo(count, buffer, sink);
                } catch (IOException e) {
                    ChannelListeners.invokeChannelExceptionHandler(source, readExceptionHandler, e);
                    return;
                }
                if (transferred == 0 && !buffer.hasRemaining()) {
                    break;
                }
                if (transferred == -1) {
                    if (count == Long.MAX_VALUE) {
                        Channels.setReadListener(source, sourceListener);
                        if (sourceListener == null) {
                            source.suspendReads();
                        } else {
                            source.wakeupReads();
                        }

                        Channels.setWriteListener(sink, sinkListener);
                        if (sinkListener == null) {
                            sink.suspendWrites();
                        } else {
                            sink.wakeupWrites();
                        }
                    } else {
                        source.suspendReads();
                        sink.suspendWrites();
                        ChannelListeners.invokeChannelExceptionHandler(source, readExceptionHandler, new EOFException());
                    }
                    return;
                }
                if (count != Long.MAX_VALUE) {
                    count -= transferred;
                }
                while (buffer.hasRemaining()) {
                    final int res;
                    try {
                        res = sink.write(buffer);
                    } catch (IOException e) {
                        ChannelListeners.invokeChannelExceptionHandler(sink, writeExceptionHandler, e);
                        return;
                    }
                    if (res == 0) {
                        // write first listener
                        final TransferListener<I, O> listener = new TransferListener<I, O>(count, allocated, source, sink, sourceListener, sinkListener, writeExceptionHandler, readExceptionHandler, 1);
                        source.suspendReads();
                        source.getReadSetter().set(listener);
                        // flush the write channel
                        sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(listener, new ChannelExceptionHandler<Channel>() {
                            @Override
                            public void handleException(Channel channel, IOException exception) {
                                listener.writeFailed(exception);
                            }
                        }));
                        sink.resumeWrites();
                        free = false;
                        return;
                    } else if (count != Long.MAX_VALUE) {
                        count -= res;
                    }
                }
                if (count == 0) {
                    //we are done
                    Channels.setReadListener(source, sourceListener);
                    if (sourceListener == null) {
                        source.suspendReads();
                    } else {
                        source.wakeupReads();
                    }

                    Channels.setWriteListener(sink, sinkListener);
                    if (sinkListener == null) {
                        sink.suspendWrites();
                    } else {
                        sink.wakeupWrites();
                    }
                    return;
                }
            }

            // flush the write channel
            try {
                // TODO: think about checking to see if a flush has already been issued sink.isReadyForFlush()
                if (!sink.flush()) {
                    final TransferListener<I, O> listener = new TransferListener<I, O>(count, allocated, source, sink, sourceListener, sinkListener, writeExceptionHandler, readExceptionHandler, 1);
                    source.suspendReads();
                    source.getReadSetter().set(listener);
                    // flush the write channel
                    sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(listener, new ChannelExceptionHandler<Channel>() {
                        @Override
                        public void handleException(Channel channel, IOException exception) {
                            listener.writeFailed(exception);
                        }
                    }));
                    sink.resumeWrites();
                    free = false;
                    return;
                }
            } catch (IOException e) {
                ChannelListeners.invokeChannelExceptionHandler(sink, writeExceptionHandler, e);
                return;
            }

            // read first listener
            final TransferListener<I, O> listener = new TransferListener<I, O>(count, allocated, source, sink, sourceListener, sinkListener, writeExceptionHandler, readExceptionHandler, 0);
            sink.suspendWrites();
            sink.getWriteSetter().set(listener);
            source.getReadSetter().set(listener);
            source.resumeReads();
            free = false;
            return;
        } finally {
            if (free) allocated.free();
        }
    }

    static final class TransferListener<I extends StreamSourceChannel, O extends StreamSinkChannel> implements ChannelListener<Channel> {
        private final Pooled<ByteBuffer> pooledBuffer;
        private final I source;
        private final O sink;
        private final ChannelListener<? super I> sourceListener;
        private final ChannelListener<? super O> sinkListener;
        private final ChannelExceptionHandler<? super O> writeExceptionHandler;
        private final ChannelExceptionHandler<? super I> readExceptionHandler;
        private long count;
        private volatile int state;

        TransferListener(final long count, final Pooled<ByteBuffer> pooledBuffer, final I source, final O sink, final ChannelListener<? super I> sourceListener, final ChannelListener<? super O> sinkListener, final ChannelExceptionHandler<? super O> writeExceptionHandler, final ChannelExceptionHandler<? super I> readExceptionHandler, final int state) {
            this.count = count;
            this.pooledBuffer = pooledBuffer;
            this.source = source;
            this.sink = sink;
            this.sourceListener = sourceListener;
            this.sinkListener = sinkListener;
            this.writeExceptionHandler = writeExceptionHandler;
            this.readExceptionHandler = readExceptionHandler;
            this.state = state;
        }

        public void handleEvent(final Channel channel) {
            final ByteBuffer buffer = pooledBuffer.getResource();
            int state = this.state;
            // always read after and write before state
            long count = this.count;
            long lres;
            int ires;
            switch (state) {
                case 0: {
                    // read listener
                    for (;;) {
                        boolean needsFlush = false;
                        try {
                            lres = source.transferTo(count, buffer, sink);
                        } catch (IOException e) {
                            readFailed(e);
                            return;
                        }
                        if (lres == 0 && !buffer.hasRemaining()) {
                            this.count = count;
                            source.resumeReads();
                            return;
                        }
                        if (lres == -1) {
                            // possibly unexpected EOF
                            if (count == Long.MAX_VALUE) {
                                // it's OK; just be done
                                done();
                                return;
                            } else {
                                readFailed(new EOFException());
                                return;
                            }
                        }
                        if (count != Long.MAX_VALUE) {
                            count -= lres;
                        }
                        needsFlush = needsFlush || lres > 0;
                        while (buffer.hasRemaining()) {
                            try {
                                ires = sink.write(buffer);
                            } catch (IOException e) {
                                writeFailed(e);
                                return;
                            }
                            if (count != Long.MAX_VALUE) {
                                count -= ires;
                            }
                            needsFlush = needsFlush || ires > 0;
                            if (ires == 0) {
                                // flush the write channel
                                try {
                                    if (needsFlush && !sink.flush()) {
                                        sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(this, new ChannelExceptionHandler<Channel>() {
                                            @Override
                                            public void handleException(Channel channel, IOException exception) {
                                                writeFailed(exception);
                                            }
                                        }));
                                    }
                                } catch (IOException e) {
                                    writeFailed(e);
                                    return;
                                }
                                this.count = count;
                                this.state = 1;
                                source.suspendReads();
                                sink.resumeWrites();
                                return;
                            }
                        }

                        if (count == 0) {
                            done();
                            return;
                        }
                        
                        // flush the write channel
                        try {
                            if (needsFlush && !sink.flush()) {
                                sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(this, new ChannelExceptionHandler<Channel>() {
                                    @Override
                                    public void handleException(Channel channel, IOException exception) {
                                        writeFailed(exception);
                                    }
                                }));
                                this.count = count;
                                this.state = 1;
                                source.suspendReads();
                                sink.resumeWrites();
                                return;
                            }
                        } catch (IOException e) {
                            writeFailed(e);
                            return;
                        }
                    }
                }
                case 1: {
                    // write listener
                    boolean needsFlush = false;
                    for (;;) {
                        while (buffer.hasRemaining()) {
                            try {
                                ires = sink.write(buffer);
                            } catch (IOException e) {
                                writeFailed(e);
                                return;
                            }
                            if (count != Long.MAX_VALUE) {
                                count -= ires;
                            }
                            needsFlush = needsFlush || ires > 0;
                            if (ires == 0) {
                                // flush the write channel
                                try {
                                    if (!sink.flush()) {
                                        sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(this, new ChannelExceptionHandler<Channel>() {
                                            @Override
                                            public void handleException(Channel channel, IOException exception) {
                                                writeFailed(exception);
                                            }
                                        }));
                                    }
                                } catch (IOException e) {
                                    writeFailed(e);
                                }
                                source.suspendReads();
                                sink.resumeWrites();
                                return;
                            }
                        }
                        try {
                            lres = source.transferTo(count, buffer, sink);
                        } catch (IOException e) {
                            readFailed(e);
                            return;
                        }
                        needsFlush = needsFlush || lres > 0;
                        if (lres == 0 && !buffer.hasRemaining()) {
                            // flush the write channel
                            try {
                                if (needsFlush && !sink.flush()) {
                                    sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(this, new ChannelExceptionHandler<Channel>() {
                                        @Override
                                        public void handleException(Channel channel, IOException exception) {
                                            writeFailed(exception);
                                        }
                                    }));
                                    this.count = count;
                                    this.state = 1;
                                    source.suspendReads();
                                    sink.resumeWrites();
                                    return;
                                }
                            } catch (IOException e) {
                                writeFailed(e);
                                return;
                            }

                            // need more data
                            this.count = count;
                            this.state = 0;
                            sink.suspendWrites();
                            source.resumeReads();
                            return;
                        }
                        if (lres == -1) {
                            // possibly unexpected EOF
                            if (count == Long.MAX_VALUE) {
                                // it's OK; just be done
                                done();
                                return;
                            } else {
                                readFailed(new EOFException());
                                return;
                            }
                        }
                        if (count != Long.MAX_VALUE) {
                            count -= lres;
                        }

                        if (count == 0) {
                            done();
                            return;
                        }
                    }
                }
            }
        }

        private void writeFailed(final IOException e) {
            try {
                source.suspendReads();
                sink.suspendWrites();
                ChannelListeners.invokeChannelExceptionHandler(sink, writeExceptionHandler, e);
            } finally {
                pooledBuffer.free();
            }
        }

        private void readFailed(final IOException e) {
            try {
                source.suspendReads();
                sink.suspendWrites();
                ChannelListeners.invokeChannelExceptionHandler(source, readExceptionHandler, e);
            } finally {
                pooledBuffer.free();
            }
        }

        private void done() {
            try {
                final ChannelListener<? super I> sourceListener = this.sourceListener;
                final ChannelListener<? super O> sinkListener = this.sinkListener;
                final I source = this.source;
                final O sink = this.sink;

                Channels.setReadListener(source, sourceListener);
                if (sourceListener == null) {
                    source.suspendReads();
                } else {
                    source.wakeupReads();
                }

                Channels.setWriteListener(sink, sinkListener);
                if (sinkListener == null) {
                    sink.suspendWrites();
                } else {
                    sink.wakeupWrites();
                }
            } finally {
                pooledBuffer.free();
            }
        }

        public String toString() {
            return "Transfer channel listener (" + source + " to " + sink + ") -> (" + sourceListener + " and " + sinkListener + ")";
        }
    }

    private ChannelUtils() {
    }

}
