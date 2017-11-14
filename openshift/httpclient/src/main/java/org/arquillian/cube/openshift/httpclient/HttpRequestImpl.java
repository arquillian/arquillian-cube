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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class HttpRequestImpl implements HttpRequest {
    private HttpUriRequest request;

    public HttpRequestImpl(HttpUriRequest request) {
        this.request = request;
    }

    HttpUriRequest unwrap() {
        return request;
    }

    public void setHeader(String name, String value) {
        request.setHeader(name, value);
    }

    public void setEntity(String body) throws IOException {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest.class.cast(request).setEntity(new StringEntity(body));
        }
    }

    public void setEntity(Map<String, String> form) throws IOException {
        if (request instanceof HttpEntityEnclosingRequest) {
            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, String> entry : form.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            HttpEntityEnclosingRequest.class.cast(request).setEntity(new UrlEncodedFormEntity(params));
        }
    }
}
