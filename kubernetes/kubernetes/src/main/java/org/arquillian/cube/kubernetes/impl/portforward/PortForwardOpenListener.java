package org.arquillian.cube.kubernetes.impl.portforward;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.conduits.ReadTimeoutStreamSourceConduit;
import io.undertow.conduits.WriteTimeoutStreamSinkConduit;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import io.undertow.server.XnioByteBufferPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.StreamConnection;

/**
 * PortForwardOpenListener
 *
 * @author Rob Cernich
 */
public class PortForwardOpenListener implements OpenListener {

    private final ByteBufferPool bufferPool;
    private final int bufferSize;
    private final String urlPath;
    private final int targetPort;
    private final AtomicInteger requestId;
    private volatile OptionMap undertowOptions;
    private ClientConnection masterPortForwardConnection;

    public PortForwardOpenListener(ClientConnection masterPortForwardConnection, final String urlPath,
        final int targetPort, final AtomicInteger requestId, final Pool<ByteBuffer> pool,
        final OptionMap undertowOptions) {
        this.masterPortForwardConnection = masterPortForwardConnection;
        this.urlPath = urlPath;
        this.targetPort = targetPort;
        this.requestId = requestId;
        this.undertowOptions = undertowOptions;
        this.bufferPool = new XnioByteBufferPool(pool);
        Pooled<ByteBuffer> buf = pool.allocate();
        this.bufferSize = buf.getResource().remaining();
        buf.free();
    }

    @Override
    public void handleEvent(StreamConnection channel) {
        //set read and write timeouts
        try {
            Integer readTimeout = channel.getOption(Options.READ_TIMEOUT);
            Integer idleTimeout = undertowOptions.get(UndertowOptions.IDLE_TIMEOUT);
            if ((readTimeout == null || readTimeout <= 0) && idleTimeout != null) {
                readTimeout = idleTimeout;
            } else if (readTimeout != null && idleTimeout != null && idleTimeout > 0) {
                readTimeout = Math.min(readTimeout, idleTimeout);
            }
            if (readTimeout != null && readTimeout > 0) {
                channel.getSourceChannel()
                    .setConduit(
                        new ReadTimeoutStreamSourceConduit(channel.getSourceChannel().getConduit(), channel, this));
            }
            Integer writeTimeout = channel.getOption(Options.WRITE_TIMEOUT);
            if ((writeTimeout == null || writeTimeout <= 0) && idleTimeout != null) {
                writeTimeout = idleTimeout;
            } else if (writeTimeout != null && idleTimeout != null && idleTimeout > 0) {
                writeTimeout = Math.min(writeTimeout, idleTimeout);
            }
            if (writeTimeout != null && writeTimeout > 0) {
                channel.getSinkChannel()
                    .setConduit(new WriteTimeoutStreamSinkConduit(channel.getSinkChannel().getConduit(), channel, this));
            }
        } catch (IOException e) {
            IoUtils.safeClose(channel);
            UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
        }

        final PortForwardServerConnection connection =
            new PortForwardServerConnection(channel, bufferPool, undertowOptions, bufferSize);
        connection.getWorker().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.startForwarding(masterPortForwardConnection, urlPath, targetPort,
                        requestId.getAndIncrement());
                } catch (IOException e) {
                } finally {
                    IoUtils.safeClose(connection);
                }
            }
        });
    }

    @Override
    public HttpHandler getRootHandler() {
        return null;
    }

    @Override
    public void setRootHandler(HttpHandler rootHandler) {
    }

    @Override
    public OptionMap getUndertowOptions() {
        return undertowOptions;
    }

    @Override
    public void setUndertowOptions(OptionMap undertowOptions) {
        if (undertowOptions == null) {
            throw UndertowMessages.MESSAGES.argumentCannotBeNull("undertowOptions");
        }
        this.undertowOptions = undertowOptions;
    }

    @Override
    public ByteBufferPool getBufferPool() {
        return bufferPool;
    }

    @Override
    public ConnectorStatistics getConnectorStatistics() {
        return null;
    }
}
