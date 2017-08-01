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
package org.arquillian.cube.openshift.impl.client;

import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.kubernetes.api.ConfigurationFactory;
import org.arquillian.cube.kubernetes.impl.DefaultConfigurationFactory;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;

public class CubeOpenShiftConfigurationFactory extends DefaultConfigurationFactory<CubeOpenShiftConfiguration>
    implements ConfigurationFactory<CubeOpenShiftConfiguration> {

    private static final String OPENSHIFT_EXTENSION_NAME = "openshift";

    @Override
    public CubeOpenShiftConfiguration create(ArquillianDescriptor arquillian) {
        Map<String, String> config = new HashMap<>();
        config.putAll(arquillian.extension(KUBERNETES_EXTENSION_NAME).getExtensionProperties());
        config.putAll(arquillian.extension(OPENSHIFT_EXTENSION_NAME).getExtensionProperties());

        configureProtocolHandlers(config);
        return CubeOpenShiftConfiguration.fromMap(config);
    }
}
