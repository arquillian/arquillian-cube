package org.arquillian.cube.openshift.impl.client;

import io.undertow.UndertowMessages;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.protocols.spdy.SpdyStreamStreamSinkChannel;
import io.undertow.server.AbstractServerConnection;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.server.SSLSessionInfo;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.xnio.ChainedChannelListener;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioIoThread;
import org.xnio.channels.CloseableChannel;
import org.xnio.conduits.StreamSinkConduit;

/**
 * PortForwardServerConnection
 * 
 * @author Rob Cernich
 */
public class PortForwardServerConnection extends AbstractServerConnection {

    private final CountDownLatch errorComplete = new CountDownLatch(1);
    private final CountDownLatch responseComplete = new CountDownLatch(1);
    private final CountDownLatch requestComplete = new CountDownLatch(1);
//    private ChannelPipe<StreamConnection, StreamConnection> pipe;
    private String error;

    /**
     * Create a new PortForwardServerConnection.
     * 
     * @param channel
     * @param bufferPool
     * @param undertowOptions
     * @param bufferSize
     */
    public PortForwardServerConnection(StreamConnection channel, Pool<ByteBuffer> bufferPool,
            OptionMap undertowOptions, int bufferSize) {
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

    public void startForwarding(final ClientConnection clientConnection, final String urlPath, final int targetPort, final int requestId) throws IOException {
        try {
            //resetChannel();
//            pipe = getIoThread().createFullDuplexPipeConnection(clientConnection.getIoThread());
//            // hook up the left side
//            ChannelUtils.initiateTransfer(Long.MAX_VALUE, getChannel().getSourceChannel(), pipe.getLeftSide().getSinkChannel(), bufferPool);
//            ChannelUtils.initiateTransfer(Long.MAX_VALUE, pipe.getLeftSide().getSourceChannel(), getChannel().getSinkChannel(), bufferPool);

            // initiate the streams
            openErrorStream(clientConnection, urlPath, targetPort, requestId);
            openDataStream(clientConnection, urlPath, targetPort, requestId);
            try {
                /*
                 * wait for the request to complete. this will trigger when the
                 * client is done and the request stream closes.
                 */
                requestComplete.await();
                /* wait for the response on the error stream. */
                errorComplete.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IoUtils.safeClose(this);
//            IoUtils.safeClose(pipe.getLeftSide());
//            IoUtils.safeClose(pipe.getRightSide());
        }
    }

    private void openErrorStream(final ClientConnection clientConnection, final String urlPath, final int targetPort, final int requestId) throws IOException {
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
            }

            @Override
            public void completed(final ClientExchange result) {
                try {
                    flushRequest(result);
                } catch (IOException e) {
                    holder[0] = e;
                }
                latch.countDown();
                if (holder[0] != null) {
                    return;
                }
                
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        // read the error, if any
                        getWorker().execute( new Runnable() {
                            @Override
                            public void run() {
                                new StringReadChannelListener(getBufferPool()) {
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
                        });
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
    
    private void openDataStream(final ClientConnection clientConnection, final String urlPath, final int targetPort, final int requestId) throws IOException {
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
        
        getChannel().getCloseSetter().set(
                new ChainedChannelListener<CloseableChannel>(new CancelTimerChannelListener(timer),
                        new LatchReleaseChannelListener(requestComplete)));

        clientConnection.sendRequest(request, new ClientCallback<ClientExchange>() {
            @Override
            public void failed(IOException e) {
                holder[0] = e;
                latch.countDown();
            }

            @Override
            public void completed(final ClientExchange result) {
                try {
                    flushRequest(result);
                } catch (IOException e) {
                    holder[0] = e;
                }
                latch.countDown();
                if (holder[0] != null) {
                    return;
                }

                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        result.getResponseChannel().getCloseSetter().set(new LatchReleaseChannelListener(requestComplete));
                        
                        getWorker().execute( new Runnable() {
                            @Override
                            public void run() {
                                // read from remote
                                ChannelUtils.initiateTransfer(
                                        Long.MAX_VALUE,
                                        result.getResponseChannel(),
                                        getChannel().getSinkChannel(),
//                                        pipe.getRightSide().getSinkChannel(),
                                        getBufferPool());
                            }
                        });
                    }

                    @Override
                    public void failed(IOException e) {
                        requestComplete.countDown();
                    }
                });
                
                // write to remote
                ChannelUtils.initiateTransfer(
                        Long.MAX_VALUE,
                        getChannel().getSourceChannel(),
//                        pipe.getRightSide().getSourceChannel(),
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

    private void flushRequest(final ClientExchange result) throws IOException {
        /*
         * We do this in the IO thread because we can't invoke flush() a second
         * time if it fails. SPDY stream sink will only flush an empty buffer on
         * the first flush call. If the flush fails, we need to invoke it a
         * second time through a callback handler and that won't do anything
         * because the buffer is empty and it's not the first flush, causing the
         * channel to hang. :(
         */
        if (XnioIoThread.currentThread() == null) {
            final IOException[] holder = new IOException[1];
            final CountDownLatch openLatch = new CountDownLatch(1);
            getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        result.getRequestChannel().flush();
                    } catch (IOException e) {
                        holder[0] = e;
                    } finally {
                        openLatch.countDown();
                    }
                }
            });
            try {
                openLatch.await();
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while opening SPDY channel", e);
            }
            if (holder[0] != null) {
                throw new IOException(holder[0]);
            }
        } else {
            result.getRequestChannel().flush();
        }
    }

    private void setError(String error) {
        this.error = error;
        errorComplete.countDown();
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
}
