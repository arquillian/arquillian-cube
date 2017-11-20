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
import java.io.InputStream;
import org.apache.http.Header;
import org.apache.http.util.EntityUtils;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class HttpResponseImpl implements HttpResponse {
    private org.apache.http.HttpResponse response;

    public HttpResponseImpl(org.apache.http.HttpResponse response) {
        this.response = response;
    }

    public String getHeader(String name) {
        Header header = response.getFirstHeader(name);
        return (header != null ? header.getValue() : null);
    }

    public String[] getHeaders(String name) {
        Header[] headers = response.getHeaders(name);
        if (headers == null) {
            return null;
        }
        String[] values = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            values[i] = headers[i].getValue();
        }
        return values;
    }

    public int getResponseCode() {
        return response.getStatusLine().getStatusCode();
    }

    public String getResponseBodyAsString() throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    public InputStream getResponseAsStream() throws IOException {
        return response.getEntity().getContent();
    }
}
