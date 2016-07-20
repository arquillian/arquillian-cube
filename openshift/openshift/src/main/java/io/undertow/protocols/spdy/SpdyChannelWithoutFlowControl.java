package io.undertow.protocols.spdy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.StreamConnection;

public class SpdyChannelWithoutFlowControl extends SpdyChannel {

    private int initialWindowSize;
    private int currentWindowSize;

    public SpdyChannelWithoutFlowControl(StreamConnection connectedStreamChannel, Pool<ByteBuffer> bufferPool,
            Pooled<ByteBuffer> data, Pool<ByteBuffer> heapBufferPool, boolean clientSide) {
        super(connectedStreamChannel, bufferPool, data, heapBufferPool, clientSide);
        currentWindowSize = initialWindowSize = getInitialWindowSize();
    }

    @Override
    synchronized int grabFlowControlBytes(final int bytesToGrab) {
        //Doing by this way the window will always have space so the connection will not hang anymore.
        currentWindowSize += initialWindowSize;
        updateSettings(Collections.singletonList(new SpdySetting(0, SpdySetting.SETTINGS_INITIAL_WINDOW_SIZE, currentWindowSize)));
        // no flow control
        return super.grabFlowControlBytes(bytesToGrab);
    }
}