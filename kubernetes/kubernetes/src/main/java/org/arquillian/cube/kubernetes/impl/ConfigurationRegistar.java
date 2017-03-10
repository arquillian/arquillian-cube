/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.ConfigurationFactory;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class ConfigurationRegistar {

    private static final String EXTENSION_NAME = "kubernetes";

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Inject
    @ApplicationScoped
    private InstanceProducer<Configuration> configurationProducer;

    public void configure(@Observes ArquillianDescriptor arquillian) {
        ConfigurationFactory configurationFactory = serviceLoader.get().onlyOne(ConfigurationFactory.class);
        configurationProducer.set(configurationFactory.create(arquillian));
    }
}
