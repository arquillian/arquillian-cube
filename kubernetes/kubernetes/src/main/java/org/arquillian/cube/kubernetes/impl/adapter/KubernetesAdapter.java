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

package org.arquillian.cube.kubernetes.impl.adapter;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class KubernetesAdapter implements Adapter {

    private final KubernetesClient client;

    public KubernetesAdapter(KubernetesClient kubernetesClient) {
        this.client = kubernetesClient;
    }

    @Override
    public void createResource(InputStream stream) throws IOException {
        List<HasMetadata> hasMetadata = client.load(stream).createOrReplace();
    }

    @Override
    public void deleteResources(HasMetadata hasMetadata) {
        client.resourceList(hasMetadata).delete();
    }

    @Override
    public Object addRoleBinding(String resourcesKey, String roleRefName, String userName) {
        return null;
    }

    @Override
    public Object getService(String namespace, String serviceName) {
        return client.services().inNamespace(namespace).withName(serviceName).get();
    }

    @Override
    public KubernetesClient getKubernetesClient() {
        return client;
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
