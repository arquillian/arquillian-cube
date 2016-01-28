package io.undertow.protocols.spdy;

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
    int grabFlowControlBytes(final int bytesToGrab) {
        // no flow control
        final int bytesGrabbed = super.grabFlowControlBytes(bytesToGrab);
        if (bytesGrabbed < bytesToGrab || bytesToGrab == 0) {
            getWorker().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (SpdyChannelWithoutFlowControl.this) {
                            currentWindowSize += initialWindowSize;
                            updateSettings(Collections.singletonList(new SpdySetting(0, SpdySetting.SETTINGS_INITIAL_WINDOW_SIZE, currentWindowSize)));
                            handleWindowUpdate(0, initialWindowSize);
                            notifyFlowControlAllowed();
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
        return bytesGrabbed;
    }
    
}
