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

package org.arquillian.cube.openshift.impl.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Properties;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.api.ConfigurationHandle;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class RemoteConfigurationResourceProvider implements ResourceProvider {
    private static final Logger log = Logger.getLogger(RemoteConfigurationResourceProvider.class.getName());

    public static final String FILE_NAME = "ce-arq-configuration.properties";

    private static String check(String value) {
        return (value != null) ? value : "<NONE_CONFIGURED>";
    }

    public static String toProperties(ConfigurationHandle configuration) {
        try {
            Properties properties = new Properties();
            properties.put("kubernetes.master", check(configuration.getKubernetesMaster()));
            properties.put("kubernetes.api.version", check(configuration.getApiVersion()));
            properties.put("kubernetes.namespace", check(configuration.getNamespace()));
            properties.put("kubernetes.auth.token", check(configuration.getToken()));
            StringWriter writer = new StringWriter();
            properties.store(writer, "CE Arquillian Configuration");
            log.info(String.format("Stored configuration %s ...", properties));
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean canProvide(Class<?> type) {
        return ConfigurationHandle.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        final Properties properties = new Properties();
        try {
            ClassLoader cl = RemoteConfigurationResourceProvider.class.getClassLoader();
            InputStream stream = cl.getResourceAsStream(FILE_NAME);
            if (stream == null) {
                throw new IllegalArgumentException(String.format("Missing %s file (%s) ...", FILE_NAME, cl));
            }
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return new ConfigurationHandle() {
            public String getKubernetesMaster() {
                return properties.getProperty("kubernetes.master");
            }

            public String getApiVersion() {
                return properties.getProperty("kubernetes.api.version");
            }

            public String getNamespace() {
                return properties.getProperty("kubernetes.namespace");
            }

            public String getToken() {
            	return properties.getProperty("kubernetes.auth.token");
            }
        };
    }
}
