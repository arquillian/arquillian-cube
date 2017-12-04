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

package org.arquillian.cube.openshift.impl.adapter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.openshift.api.OpenShiftHandle;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.proxy.Proxy;
import org.arquillian.cube.openshift.impl.utils.Operator;
import org.arquillian.cube.openshift.impl.utils.ParamValue;
import org.arquillian.cube.openshift.impl.utils.RCContext;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface OpenShiftAdapter extends Closeable, OpenShiftHandle {
    Proxy getProxy();

    /**
     * @return true if the project was created; false if the project already exists
     */
    boolean checkProject();

    boolean deleteProject();

    String deployPod(String name, String env, RCContext context) throws Exception;

    String deployReplicationController(String name, String env, RCContext context) throws Exception;

    List<? extends OpenShiftResource> processTemplateAndCreateResources(String templateKey, String templateURL,
        List<ParamValue> values, Map<String, String> labels) throws Exception;

    Object deleteTemplate(String templateKey) throws Exception;

    Object createResource(String resourcesKey, InputStream stream) throws IOException;

    Object deleteResources(String resourcesKey);

    Object addRoleBinding(String resourcesKey, String roleRefName, String userName);

    Object getService(String namespace, String serviceName);

    void cleanReplicationControllers(String... ids) throws Exception;

    void cleanPods(Map<String, String> labels) throws Exception;

    /**
     * @param op compare current number of pods vs. replicas
     */
    void delay(Map<String, String> labels, int replicas, Operator op) throws Exception;
    
    void cleanRemnants(Map<String, String> labels) throws Exception;
}
