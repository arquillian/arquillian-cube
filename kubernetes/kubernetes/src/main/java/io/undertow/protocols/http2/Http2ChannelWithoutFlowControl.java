package io.undertow.protocols.http2;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.UndertowLogger;

import java.io.IOException;
import java.util.Collections;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;

public class Http2ChannelWithoutFlowControl extends Http2Channel {

    private int initialWindowSize;
    private int currentWindowSize;

    public Http2ChannelWithoutFlowControl(StreamConnection connectedStreamChannel, ByteBufferPool bufferPool,
        PooledByteBuffer data, ByteBufferPool heapBufferPool, boolean clientSide, OptionMap options) {
        super(connectedStreamChannel, null,  bufferPool, data, clientSide, false,
            false,  heapBufferPool.allocate().getBuffer(),options);
        currentWindowSize = initialWindowSize = getInitialSendWindowSize();
    }

    @Override
    synchronized int grabFlowControlBytes(final int bytesToGrab) {
        //Doing by this way the window will always have space so the connection will not hang anymore.
        currentWindowSize += bytesToGrab;
        updateSettings(
            Collections.singletonList(new Http2Setting(Http2Setting.SETTINGS_INITIAL_WINDOW_SIZE, currentWindowSize)));
        try {
            super.updateReceiveFlowControlWindow(currentWindowSize);
        } catch (IOException e) {
            UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
        }
        // no flow control
        return super.grabFlowControlBytes(bytesToGrab);
    }
}
