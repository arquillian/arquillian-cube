/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.arquillian.cube.openshift.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Properties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Useful methods that can also be used in-container.
 * (keep dependecies to a minimal)
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class Tools {

    /**
     * Load properties.
     *
     * @param clazz    the class from classpath where the properties are
     * @param fileName properties file name
     * @return properties
     * @throws IOException for any error
     */
    public static Properties loadProperties(Class<?> clazz, String fileName) throws IOException {
        Properties properties = new Properties();
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(fileName)) {
            properties.load(is);
        }
        return properties;
    }

    /**
     * Trust all certs.
     *
     * @throws Exception for any error
     */
    public static SSLContext trustAllCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(createSSLSocketFactory(sc));
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        return sc;
    }

    /*
    * Configure the SSLContext to accept untrusted connections
    */
    public static SSLContext getTlsSslContext() throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init( null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    private static SSLSocketFactory createSSLSocketFactory(final SSLContext sc) {
        try {
            Class.forName("javax.net.ssl.SNIHostName");
        } catch (ClassNotFoundException e) {
            // Java 7, no need to patch
            return sc.getSocketFactory();
        }
        return new DelegatingSSLSocketFactory(sc.getSocketFactory());
    }

    private static final class DelegatingSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        
        private DelegatingSSLSocketFactory(final SSLSocketFactory delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return overrideHostname(delegate.createSocket(s, host, port, autoClose), host);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return overrideHostname(delegate.createSocket(host, port), host);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException,
                UnknownHostException {
            return overrideHostname(delegate.createSocket(host, port, localHost, localPort), host);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return overrideHostname(delegate.createSocket(host, port), host.getHostName());
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                throws IOException {
            return overrideHostname(delegate.createSocket(address, port, localAddress, localPort), address.getHostName());
        }
        
        private Socket overrideHostname(final Socket socket, String hostname) {
            if (hostname == null) {
                return socket;
            }
            final SSLSocket sslSocket = (SSLSocket) socket;
            final SSLParameters params = sslSocket.getSSLParameters();
            params.setServerNames(Collections.<SNIServerName>singletonList(new SNIHostName(hostname)));
            sslSocket.setSSLParameters(params);
            return sslSocket;
        }
    }
}
