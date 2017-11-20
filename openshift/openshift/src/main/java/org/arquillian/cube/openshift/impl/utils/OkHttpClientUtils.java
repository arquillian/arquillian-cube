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

package org.arquillian.cube.openshift.impl.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Handle OkHttpClient.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class OkHttpClientUtils {
    private static final SimpleCookieJar COOKIE_JAR = new SimpleCookieJar();

    public static void applyConnectTimeout(OkHttpClient.Builder builder, long timeout) {
        //Increasing timeout to avoid this issue:
        //Caused by: io.fabric8.kubernetes.client.KubernetesClientException: Error executing: GET at:
        //https://localhost:8443/api/v1/namespaces/cearq-jws-tcznhcfw354/pods?labelSelector=deploymentConfig%3Djws-app. Cause: timeout
        builder.connectTimeout(timeout, TimeUnit.SECONDS);
    }

    public static void applyCookieJar(OkHttpClient.Builder builder) {
        COOKIE_JAR.clear(); // reset
        builder.cookieJar(COOKIE_JAR);
    }

    /**
     * Just copy cookies based on proxy path.
     */
    private static class SimpleCookieJar implements CookieJar {
        private static final String _PROXY = "/proxy";
        private Map<String, List<Cookie>> cookiesMap = new ConcurrentHashMap<>();

        private void clear() {
            cookiesMap.clear();
        }

        private static String path(HttpUrl url) {
            String path = url.encodedPath();
            int p = path.indexOf(_PROXY);
            return path.substring(p + _PROXY.length());
        }

        public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookiesMap.put(path(url), cookies);
        }

        public synchronized List<Cookie> loadForRequest(HttpUrl url) {
            String path = path(url);
            List<Cookie> list = new ArrayList<>();
            for (Map.Entry<String, List<Cookie>> entry : cookiesMap.entrySet()) {
                if (path.startsWith(entry.getKey())) {
                    for (Cookie cookie : entry.getValue()) {
                        list.add(cookie);
                    }
                }
            }
            return list.isEmpty() ? Collections.<Cookie>emptyList() : list;
        }
    }
}
