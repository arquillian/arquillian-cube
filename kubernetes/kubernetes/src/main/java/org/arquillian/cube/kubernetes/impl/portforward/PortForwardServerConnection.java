package org.arquillian.cube.kubernetes.impl.portforward;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.xnio.ChainedChannelListener;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.channels.CloseableChannel;
import org.xnio.conduits.StreamSinkConduit;

import io.undertow.UndertowMessages;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.spdy.SpdyStreamStreamSinkChannel;
import io.undertow.server.AbstractServerConnection;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.server.SSLSessionInfo;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;

/**
 * PortForwardServerConnection
 *
 * @author Rob Cernich
 */
public class PortForwardServerConnection extends AbstractServerConnection {

    private final CountDownLatch errorComplete = new CountDownLatch(1);
    private final CountDownLatch requestComplete = new CountDownLatch(1);

    /**
     * Create a new PortForwardServerConnection.
     */
    public PortForwardServerConnection(StreamConnection channel, ByteBufferPool bufferPool, OptionMap undertowOptions,
                                       int bufferSize) {
        super(channel, bufferPool, null, undertowOptions, bufferSize);
    }

    @Override
    public HttpServerExchange sendOutOfBandResponse(HttpServerExchange exchange) {
        throw new UnsupportedOperationException("PortForward connection does not support HTTP!");
    }

    @Override
    public void terminateRequestChannel(HttpServerExchange exchange) {
        throw new UnsupportedOperationException("PortForward connection does not support HTTP!");
    }

    @Override
    public SSLSessionInfo getSslSessionInfo() {
        // We're not supporting SSL
        return null;
    }

    @Override
    public void setSslSessionInfo(SSLSessionInfo sessionInfo) {
        throw new UnsupportedOperationException("PortForward connection does not support SSL!");
    }

    @Override
    protected StreamConnection upgradeChannel() {
        throw UndertowMessages.MESSAGES.upgradeNotSupported();
    }

    @Override
    protected StreamSinkConduit getSinkConduit(HttpServerExchange exchange, StreamSinkConduit conduit) {
        return conduit;
    }

    @Override
    protected boolean isUpgradeSupported() {
        return false;
    }

    @Override
    protected void exchangeComplete(HttpServerExchange exchange) {
        // We're not supporting HTTP so nothing to do here
    }

    @Override
    public String getTransportProtocol() {
        return "raw";
    }

    @Override
    protected boolean isConnectSupported() {
        return false;
    }

    @Override
    public boolean isContinueResponseSupported() {
        return false;
    }

    @Override
    protected void setConnectListener(HttpUpgradeListener connectListener) {
    }

    public void startForwarding(final ClientConnection clientConnection, final String urlPath, final int targetPort,
                                final int requestId) throws IOException {
        try {
            // initiate the streams
            openErrorStream(clientConnection, urlPath, targetPort, requestId);
            openDataStream(clientConnection, urlPath, targetPort, requestId);
            try {
                /*
                 * wait for the request to complete. this will trigger when the
                 * client is done and the request stream closes.
                 */
                requestComplete.await();
                /* wait for the response on the error stream.
                *  Mont of times the connection hangs here,
                * */
                errorComplete.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IoUtils.safeClose(this);
        }
    }

    private void openErrorStream(final ClientConnection clientConnection, final String urlPath, final int targetPort,
                                 final int requestId) throws IOException {
        ClientRequest request = new ClientRequest()
            .setMethod(Methods.POST)
            .setPath(urlPath);
        request.getRequestHeaders()
            .put(new HttpString("streamType"), "error")
            .put(new HttpString("port"), targetPort)
            .put(new HttpString("requestID"), requestId);
        final CountDownLatch latch = new CountDownLatch(1);
        final IOException[] holder = new IOException[1];
        clientConnection.sendRequest(request, new ClientCallback<ClientExchange>() {
            @Override
            public void failed(IOException e) {
                holder[0] = e;
                latch.countDown();
                errorComplete.countDown();
                requestComplete.countDown();
            }

            @Override
            public void completed(final ClientExchange result) {
                latch.countDown();

                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        // read the error, if any
                        new StringReadChannelListener(getByteBufferPool()) {
                            @Override
                            protected void stringDone(String string) {
                                setError(string);
                            }

                            @Override
                            protected void error(IOException e) {
                                setError(e.getMessage());
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        setError(e.getMessage());
                    }
                });
            }
        });
        try {
            // wait for the request to be sent
            latch.await();
        } catch (InterruptedException e) {
        }
        if (holder[0] != null) {
            throw holder[0];
        }
    }

    private void openDataStream(final ClientConnection clientConnection, final String urlPath, final int targetPort,
                                final int requestId) throws IOException {
        ClientRequest request = new ClientRequest()
            .setMethod(Methods.POST)
            .setPath(urlPath);
        request.getRequestHeaders()
            .put(new HttpString("streamType"), "data")
            .put(new HttpString("port"), targetPort)
            .put(new HttpString("requestID"), requestId);
        final CountDownLatch latch = new CountDownLatch(1);
        final IOException[] holder = new IOException[1];
        final Timer timer = new Timer("SPDY Keep Alive", true);

        getChannel().getCloseSetter()
            .set(new ChainedChannelListener<CloseableChannel>(
                new CancelTimerChannelListener(timer),
                new LatchReleaseChannelListener(requestComplete),
                new LatchReleaseChannelListener(errorComplete)));

        clientConnection.sendRequest(request, new ClientCallback<ClientExchange>() {
            @Override
            public void failed(IOException e) {
                holder[0] = e;
                latch.countDown();
                errorComplete.countDown();
                requestComplete.countDown();
            }

            @Override
            public void completed(final ClientExchange result) {
                latch.countDown();

                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        result.getResponseChannel()
                            .getCloseSetter()
                            .set(new ChainedChannelListener<CloseableChannel>(
                                    new CancelTimerChannelListener(timer),
                                    new LatchReleaseChannelListener(requestComplete),
                                    new LatchReleaseChannelListener(errorComplete)));
                        getIoThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                // read from remote
                                ChannelUtils.initiateTransfer(
                                    Long.MAX_VALUE,
                                    result.getResponseChannel(),
                                    getChannel().getSinkChannel(),
                                    getBufferPool());
                            }
                        });
                    }

                    @Override
                    public void failed(IOException e) {
                        requestComplete.countDown();
                        errorComplete.countDown();
                    }
                });

                // write to remote
                ChannelUtils.initiateTransfer(
                    Long.MAX_VALUE,
                    getChannel().getSourceChannel(),
                    result.getRequestChannel(),
                    getBufferPool());

                // keep alive
                timer.scheduleAtFixedRate(new PingSpdyStream((SpdyStreamStreamSinkChannel) result.getRequestChannel()),
                    15000, 15000); // OSE times out after 30s

                // need to wait for the client to close the request channel
                try {
                    requestComplete.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    errorComplete.countDown();
                }
            }
        });
        try {
            // wait for the request to be sent
            latch.await();
        } catch (InterruptedException e) {
        }
        if (holder[0] != null) {
            throw holder[0];
        }
    }

    private void setError(String error) {
        if (error != null && !error.trim().equals("")) {
            System.err.println("Port forwarding error: " + error);
        }
        errorComplete.countDown();
        requestComplete.countDown();
    }

    private static final class CancelTimerChannelListener implements ChannelListener<CloseableChannel> {

        private final Timer timer;

        private CancelTimerChannelListener(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void handleEvent(CloseableChannel channel) {
            timer.cancel();
        }
    }

    private static final class LatchReleaseChannelListener implements ChannelListener<CloseableChannel> {

        private final CountDownLatch latch;

        private LatchReleaseChannelListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void handleEvent(CloseableChannel channel) {
            latch.countDown();
        }
    }

    private final class PingSpdyStream extends TimerTask {

        private final SpdyStreamStreamSinkChannel stream;

        private PingSpdyStream(SpdyStreamStreamSinkChannel stream) {
            super();
            this.stream = stream;
        }

        @Override
        public void run() {
            getWorker().execute(new Runnable() {
                @Override
                public void run() {
                    if (stream.isOpen()) {
                        stream.getChannel().sendPing(stream.getStreamId());
                    }
                }
            });
        }
    }
}
