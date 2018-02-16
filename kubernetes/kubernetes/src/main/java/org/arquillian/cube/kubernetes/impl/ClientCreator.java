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

import io.fabric8.kubernetes.api.builder.v3_1.TypedVisitor;
import io.fabric8.kubernetes.clnt.v3_1.ConfigBuilder;
import io.fabric8.kubernetes.clnt.v3_1.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import org.arquillian.cube.impl.util.Strings;
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

        final ConfigBuilder configBuilder = new ConfigBuilder()
            .withNamespace(config.getNamespace())
            .withApiVersion(config.getApiVersion())
            .withTrustCerts(config.isTrustCerts())
            .accept(new TypedVisitor<ConfigBuilder>() {
                @Override
                public void visit(ConfigBuilder b) {
                    b.withNoProxy(b.getNoProxy() == null ? new String[0] : b.getNoProxy());
                }
            });

        if (Strings.isNotNullOrEmpty(config.getMasterUrl().toString())) {
            configBuilder.withMasterUrl(config.getMasterUrl().toString());
        }

        if (Strings.isNotNullOrEmpty(config.getToken())) {
            configBuilder.withOauthToken(config.getToken());
        }

        if (Strings.isNotNullOrEmpty(config.getUsername()) && Strings.isNotNullOrEmpty(config.getPassword())) {
            configBuilder.withUsername(config.getUsername())
                .withPassword(config.getPassword());
        }

        producer.set(new DefaultKubernetesClient(configBuilder.build()));
    }
}
