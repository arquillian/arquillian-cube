/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other
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

package org.arquillian.cube.openshift.httpclient;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class HttpClientImpl implements HttpClient {
    private CloseableHttpClient client;

    public HttpClientImpl(CloseableHttpClient client) {
        this.client = client;
    }

    public HttpResponse execute(HttpRequest request) throws IOException {
        return execute(request, new HttpClientExecuteOptions.Builder().build());
    }

    public HttpResponse execute(HttpRequest request, HttpClientExecuteOptions options) throws IOException {
        IOException exception = null;
        HttpUriRequest r = HttpRequestImpl.class.cast(request).unwrap();
        CloseableHttpResponse rawResponse = null;
        HttpResponse response = null;

        for (int i = 0; i < options.getTries(); i++) {
            try {
                if (rawResponse != null) {
                    EntityUtils.consume(rawResponse.getEntity());
                }
                rawResponse = client.execute(r);
                response = new HttpResponseImpl(rawResponse);
                if (options.getDesiredStatusCode() == -1 || response.getResponseCode() == options.getDesiredStatusCode()) {
                    return response;
                }
                System.err.println(String.format("Response error [URL:%s]: Got code %d, expected %d.", r.getURI(),
                        response.getResponseCode(), options.getDesiredStatusCode()));
            } catch (IOException e) {
                exception = e;
                System.err.println(String.format("Execute error [URL:%s]: %s.", r.getURI(), e));
            }

            if (i + 1 < options.getTries()) {
                System.err.println(String.format("Trying again in %d seconds.", options.getDelay()));
                try {
                    Thread.sleep(options.getDelay() * 1000);
                } catch (InterruptedException e) {
                    exception = new IOException(e);
                    break;
                }
            } else {
                System.err.println(String.format("Giving up trying URL:%s after %d tries", r.getURI(), options.getTries()));
            }
        }

        if (exception != null) {
            throw exception;
        }

        return response;
    }

    public void close() throws IOException {
        client.close();
    }
}
