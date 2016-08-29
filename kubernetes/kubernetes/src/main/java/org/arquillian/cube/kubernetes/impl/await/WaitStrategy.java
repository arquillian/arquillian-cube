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
package org.arquillian.cube.kubernetes.impl.await;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Session;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public class WaitStrategy implements Visitor {

    private static final int POD_READY = 1;
    private static final int SERIVCE_READY = 2;


    private final KubernetesClient client;
    private final Session session;
    private final Configuration configuration;

    private final Callable<Boolean> sessionPodsReady;
    private final Callable<Boolean> servicesReady;

    private final Map<Integer, Callable<Boolean>> conditions = new TreeMap<>();

    public WaitStrategy(KubernetesClient client, Session session, Configuration configuration) {
        this.client = client;
        this.session = session;
        this.configuration = configuration;
        this.sessionPodsReady = new SessionPodsAreReady(client, session);
        this.servicesReady = new SessionServicesAreReady(client, session, configuration);
    }

    public boolean await() throws Exception {
        long start = System.currentTimeMillis();
        Callable<Boolean> condition = new CompositeCondition(conditions.values());
        while (!Thread.interrupted() && System.currentTimeMillis() - start <= configuration.getWaitTimeout()) {
            try {
                if (condition.call()) {
                    return true;
                } else {
                    Thread.sleep(configuration.getWaitPollInterval());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    @Override
    public void visit(Object o) {
        if (o instanceof ContainerBuilder) {
            conditions.put(POD_READY, sessionPodsReady);
        } else if (o instanceof ServiceBuilder) {
            conditions.put(SERIVCE_READY, servicesReady);
        }
    }
}
