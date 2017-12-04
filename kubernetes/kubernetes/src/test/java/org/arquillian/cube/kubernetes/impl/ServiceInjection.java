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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class ServiceInjection {

    @ArquillianResource
    private KubernetesClient client;

    @ArquillianResource
    private ServiceList serviceList;

    @Named("test-service")
    @ArquillianResource
    private Service service;

    @Named(value = "test-service-second", namespace = "test-secondary-namespace")
    @ArquillianResource
    private Service serviceInSecondaryNamespace;

    @Test
    public void testServicesInjection() {
        assertNotNull(serviceList);
        assertEquals(1, serviceList.getItems().size());
        assertEquals("test-service", serviceList.getItems().get(0).getMetadata().getName());

        assertNotNull(service);
        assertEquals("test-service", service.getMetadata().getName());

        assertNotNull(serviceInSecondaryNamespace);
        assertEquals("test-service-second", serviceInSecondaryNamespace.getMetadata().getName());
        assertEquals("test-secondary-namespace", serviceInSecondaryNamespace.getMetadata().getNamespace());
    }
}
