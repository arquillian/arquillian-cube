/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.clnt.v4_0.Config;
import io.fabric8.kubernetes.clnt.v4_0.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v4_0.KubernetesClient;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Creates an instances of the {@link KubernetesClient} when a {@link @Configuration} is available.
 */
public class ClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<KubernetesClient> producer;

    public void createClient(@Observes Configuration config) {
        final Config buildConfig = new ClientConfigBuilder().configuration(config).build();

        producer.set(new DefaultKubernetesClient(buildConfig));
    }
}
