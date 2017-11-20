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

package org.arquillian.cube.openshift.impl.proxy;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import org.arquillian.cube.openshift.api.ManagementHandle;
import org.arquillian.cube.openshift.impl.portfwd.PortForward;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface Proxy {
    ManagementHandle createManagementHandle(Map<String, String> labels);

    SSLContext getSSLContext();

    void setDefaultSSLContext();

    PortForward createPortForward();

    String url(String podName, String protocol, int port, String path, String parameters);

    String url(String podName, int port, String path, String parameters);

    String url(Map<String, String> labels, int index, int port, String path, String parameters);

    Set<String> getReadyPods(Map<String, String> labels);

    <T> T post(String url, Class<T> returnType, Object requestObject) throws Exception;

    InputStream post(String url, String encoding, byte[] bytes) throws Exception;

    InputStream post(String podName, int port, String path) throws Exception;

    InputStream post(Map<String, String> labels, int index, int port, String path) throws Exception;

    int status(String url);

    String findPod(Map<String, String> labels, int index);
}
