package io.undertow.protocols.spdy;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import java.util.Collections;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;

public class SpdyChannelWithoutFlowControl extends SpdyChannel {

    private int initialWindowSize;
    private int currentWindowSize;

    public SpdyChannelWithoutFlowControl(StreamConnection connectedStreamChannel, ByteBufferPool bufferPool,
        PooledByteBuffer data, ByteBufferPool heapBufferPool, boolean clientSide, OptionMap options) {
        super(connectedStreamChannel, bufferPool, data, heapBufferPool, clientSide, options);
        currentWindowSize = initialWindowSize = getInitialWindowSize();
    }

    @Override
    synchronized int grabFlowControlBytes(final int bytesToGrab) {
        //Doing by this way the window will always have space so the connection will not hang anymore.
        currentWindowSize += bytesToGrab;
        updateSettings(
            Collections.singletonList(new SpdySetting(0, SpdySetting.SETTINGS_INITIAL_WINDOW_SIZE, currentWindowSize)));
        super.updateReceiveFlowControlWindow(currentWindowSize);
        // no flow control
        return super.grabFlowControlBytes(bytesToGrab);
    }
}
