package org.arquillian.cube.kubernetes.impl.portforward;

import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.kubernetes.clnt.v2_6.internal.SSLUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.UndertowClient;
import io.undertow.client.spdy.SpdyClientConnection;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.spdy.SpdyChannel;
import io.undertow.protocols.spdy.SpdyChannelWithoutFlowControl;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.ssl.XnioSsl;

public final class PortForwarder implements Closeable {

    private static final String PORT_FWD = "%sapi/v1/namespaces/%s/pods/%s/portforward";
    private static final AtomicInteger requestId = new AtomicInteger();
    private final OptionMap DEFAULT_OPTIONS;
    private InetAddress portForwardBindAddress = Inet4Address.getLoopbackAddress();
    private URI portForwardURI;
    private Pool<ByteBuffer> bufferPoolSlice;
    private ByteBufferPool bufferPool;
    private XnioWorker xnioWorker;
    private ClientConnection connection;
    private Collection<PortForwardServer> servers = new ArrayList<>();

    public PortForwarder(Config config, String podName) throws Exception {
        try {
            this.portForwardURI =
                URI.create(String.format(PORT_FWD, config.getMasterUrl(), config.getNamespace(), podName));

            final Xnio xnio = Xnio.getInstance();
            DEFAULT_OPTIONS = OptionMap.builder()
                .set(Options.WORKER_NAME, String.format("PortForwarding for %s/%s", config.getNamespace(), podName))
                .set(Options.WORKER_IO_THREADS, 16)
                .set(Options.CONNECTION_HIGH_WATER, 400)
                .set(Options.CONNECTION_LOW_WATER, 200)
                .set(Options.WORKER_TASK_CORE_THREADS, 16)
                .set(Options.WORKER_TASK_MAX_THREADS, 128)
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.SSL_PROTOCOL, "TLS")
                //.set(Options.CORK, true)
                .getMap();
            // XXX: hard-coding trust all certs
            final XnioSsl xnioSsl = xnio.getSslProvider(SSLUtils.keyManagers(config),
                new TrustManager[] {new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String s) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String s) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, DEFAULT_OPTIONS);
            this.xnioWorker = xnio.createWorker(null, DEFAULT_OPTIONS);
            bufferPoolSlice =
                new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, 17 * 1024, 17 * 1024 * 20);
            bufferPool = new XnioByteBufferPool(bufferPoolSlice);
            IoFuture<ClientConnection> connectFuture =
                UndertowClient.getInstance().connect(portForwardURI, xnioWorker, xnioSsl, bufferPool, DEFAULT_OPTIONS);
            // XXX: use timeout
            connection = connectFuture.getInterruptibly();

            // Establish the connection
            ClientRequest request = new ClientRequest()
                .setMethod(Methods.POST)
                .setPath(portForwardURI.getPath());
            request.getRequestHeaders()
                .put(Headers.HOST, this.portForwardURI.getHost())
                .put(Headers.CONNECTION, "Upgrade")
                .put(Headers.UPGRADE, "SPDY/3.1");
            if (config.getOauthToken() != null) {
                request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + config.getOauthToken());
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final IOException[] holder = new IOException[1];
            connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                @Override
                public void completed(ClientExchange result) {
                    result.setResponseListener(new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            try {
                                upgradeConnection(result);
                            } catch (Exception e) {
                                holder[0] = (IOException) new IOException("Unexpected error", e).fillInStackTrace();
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void failed(IOException e) {
                            holder[0] = e;
                            latch.countDown();
                        }
                    });
                }

                @Override
                public void failed(IOException e) {
                    holder[0] = e;
                    latch.countDown();
                }
            });
            latch.await();
            if (holder[0] != null) {
                throw new IOException("Failed to establish portforward client connection", holder[0]);
            }
        } catch (Throwable t) {
            if (connection != null) {
                IoUtils.safeClose(connection);
            }
            if (xnioWorker != null) {
                xnioWorker.shutdown();
            }
            throw t;
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 4) {
            System.out.println(
                "Usage: portforward <namespace> <pod> <source-port> <target-port> -b [optional] <bindAddress>");
            System.out.println("Example: portforward mynamespace somepod 8080 8080 -b 10.1.1.1");
            System.exit(1);
        }

        final String namespace = args[0];
        final String podName = args[1];
        final int sourcePort = Integer.valueOf(args[2]);
        final int targetPort = Integer.valueOf(args[3]);
        InetAddress bindAddress = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("-b")) {
                bindAddress = InetAddress.getByName(args[i + 1]);
            }
        }

        final Config config = new Config();
        config.setNamespace(namespace);
        final PortForwarder forwarder = new PortForwarder(config, podName);
        forwarder.setPortForwardBindAddress(bindAddress);
        final PortForwardServer server = forwarder.forwardPort(sourcePort, targetPort);
        System.in.read();
        server.close();
        forwarder.close();
    }

    public synchronized PortForwardServer forwardPort(int sourcePort, int targetPort)
        throws IllegalArgumentException, IOException {
        PortForwardServer server = new PortForwardServer(createServer(sourcePort, targetPort), targetPort);
        servers.add(server);
        return server;
    }

    public synchronized void close() {
        for (PortForwardServer server : servers) {
            IoUtils.safeClose(server.server);
        }
        servers.clear();
        IoUtils.safeClose(connection);
        connection = null;
        xnioWorker.shutdown();
        xnioWorker = null;
    }

    public void setPortForwardBindAddress(InetAddress bindAddress) {
        if (bindAddress == null) {
            bindAddress = Inet4Address.getLoopbackAddress();
        }
        portForwardBindAddress = bindAddress;
    }

    private synchronized void close(PortForwardServer server) {
        IoUtils.safeClose(server.server);
        servers.remove(server);
    }

    private AcceptingChannel<? extends StreamConnection> createServer(int sourcePort, int targetPort)
        throws IllegalArgumentException, IOException {
        OptionMap socketOptions = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 16)
            .set(Options.TCP_NODELAY, true)
            .set(Options.REUSE_ADDRESSES, true)
            .getMap();

        ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(
            new PortForwardOpenListener(connection, portForwardURI.getPath(), targetPort, requestId, bufferPoolSlice,
                OptionMap.EMPTY));
        AcceptingChannel<? extends StreamConnection> server =
            xnioWorker.createStreamConnectionServer(new InetSocketAddress(portForwardBindAddress, sourcePort),
                acceptListener, socketOptions);
        server.resumeAccepts();
        return server;
    }

    private void upgradeConnection(ClientExchange result) throws IOException {
        if (result.getResponse().getResponseCode() == 101) {
            // flush response
            new StringReadChannelListener(bufferPool) {
                @Override
                protected void stringDone(String string) {
                }

                @Override
                protected void error(IOException e) {
                }
            }.setup(result.getResponseChannel());

            // Create the upgraded SPDY connection
            ByteBufferPool heapBufferPool =
                new XnioByteBufferPool(new ByteBufferSlicePool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 8196, 8196));
            SpdyChannel spdyChannel =
                new SpdyChannelWithoutFlowControl(connection.performUpgrade(), bufferPool, null, heapBufferPool, true,
                    OptionMap.EMPTY);
            Integer idleTimeout = DEFAULT_OPTIONS.get(UndertowOptions.IDLE_TIMEOUT);
            if (idleTimeout != null && idleTimeout > 0) {
                spdyChannel.setIdleTimeout(idleTimeout);
            }
            connection = new SpdyClientConnection(spdyChannel, null);
        } else {
            throw new IOException("Failed to upgrade connection");
        }
    }

    public final class PortForwardServer {

        private final AcceptingChannel<? extends StreamConnection> server;
        private final int targetPort;

        private PortForwardServer(AcceptingChannel<? extends StreamConnection> server, int targetPort) {
            this.server = server;
            this.targetPort = targetPort;
        }

        public int getSourcePort() {
            return getLocalAddress().getPort();
        }

        public int getTargetPort() {
            return targetPort;
        }

        public InetSocketAddress getLocalAddress() {
            return server.getLocalAddress(InetSocketAddress.class);
        }

        public void close() {
            PortForwarder.this.close(this);
        }
    }
}
