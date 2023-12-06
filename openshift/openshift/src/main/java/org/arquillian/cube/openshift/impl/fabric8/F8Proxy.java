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

package org.arquillian.cube.openshift.impl.fabric8;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.fabric8.kubernetes.client.okhttp.OkHttpClientFactory;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import okhttp3.OkHttpClient;
import org.arquillian.cube.kubernetes.impl.ClientConfigBuilder;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.proxy.AbstractProxy;
import org.arquillian.cube.openshift.impl.utils.OkHttpClientUtils;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class F8Proxy extends AbstractProxy<Pod> {
    private final OpenShiftClient client;
    private HttpClient httpClient;

    public F8Proxy(CubeOpenShiftConfiguration configuration, NamespacedOpenShiftClient client) {
        super(configuration);
        this.client = client;
    }

    public SSLContext getSSLContext() {
        try {
            return SSLUtils.sslContext(client.getConfiguration());
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    protected synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            final Config httpClientConfig = new ClientConfigBuilder().configuration(configuration).build();
            final HttpClient.Factory httpClientFactory = new OkHttpClientFactory();
            HttpClient.Builder builder = httpClientFactory.newBuilder(httpClientConfig);  // clone
            OkHttpClientUtils.applyConnectTimeout(builder, configuration.getHttpClientTimeout());
            // TODO - TBD - OkHttpClientUtils.applyCookieJar(builder);
            httpClient = builder.build();
        }
        return httpClient;
    }

    protected List<Pod> getPods(Map<String, String> labels) {
        return client.pods().inNamespace(configuration.getNamespace()).withLabels(labels).list().getItems();
    }

    protected String getName(Pod pod) {
        return pod.getMetadata().getName();
    }

    protected boolean isReady(Pod pod) {
        PodStatus status = pod.getStatus();
        if (pod.getMetadata().getDeletionTimestamp() == null) {
            if ("Running".equalsIgnoreCase(status.getPhase())) {
                List<PodCondition> conditions = status.getConditions();
                if (conditions != null) {
                    for (PodCondition condition : conditions) {
                        if ("Ready".equalsIgnoreCase(condition.getType())) {
                            return "True".equalsIgnoreCase(condition.getStatus());
                        }
                    }
                }
            }
        }
        return false;
    }
}
