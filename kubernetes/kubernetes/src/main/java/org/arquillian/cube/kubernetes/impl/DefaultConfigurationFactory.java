/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may obtain a copy of the License
 * at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.ConfigurationFactory;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;

import static org.arquillian.cube.kubernetes.impl.Constants.DEFAULT_MAVEN_PROTOCOL_HANDLER;
import static org.arquillian.cube.kubernetes.impl.Constants.JAVA_PROTOCOL_HANDLER;
import static org.arquillian.cube.kubernetes.impl.Constants.PROTOCOL_HANDLERS;

public class DefaultConfigurationFactory<C extends DefaultConfiguration> implements ConfigurationFactory<C> {

    public static final String KUBERNETES_EXTENSION_NAME = "kubernetes";

    protected static void configureProtocolHandlers(Map<String, String> conf) {
        Set<String> handlers = new LinkedHashSet<>();
        handlers.addAll(Strings.splitAndTrimAsList(System.getProperty(JAVA_PROTOCOL_HANDLER, ""), " "));
        handlers.addAll(Strings.splitAndTrimAsList(
            conf.getOrDefault(PROTOCOL_HANDLERS, DEFAULT_MAVEN_PROTOCOL_HANDLER), " "));
        System.setProperty(JAVA_PROTOCOL_HANDLER, Strings.join(handlers, " "));
    }

    @Override
    public C create(ArquillianDescriptor arquillian) {
        Map<String, String> config = arquillian.extension(KUBERNETES_EXTENSION_NAME).getExtensionProperties();
        configureProtocolHandlers(config);
        final DefaultConfiguration configuration = DefaultConfiguration.fromMap(config);
        System.out.println(configuration);
        return (C)configuration;
    }
}
