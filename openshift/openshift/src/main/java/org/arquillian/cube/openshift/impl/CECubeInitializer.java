/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other
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
package org.arquillian.cube.openshift.impl;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.fabric8.F8OpenShiftAdapter;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * CECubeInitializer
 * <p/>
 * Initializer for CE Arquillian Cube extension.
 * 
 * @author Rob Cernich
 */
public class CECubeInitializer {

    @Inject
    @ApplicationScoped
    private InstanceProducer<OpenShiftAdapter> openShiftAdapterProducer;

    @Inject
    @ApplicationScoped
    private Instance<CubeOpenShiftConfiguration> configurationInstance;

    public void createOpenShiftAdapter(@Observes OpenShiftClient client, Configuration configuration) {
        CubeOpenShiftConfiguration cubeOpenShiftConfiguration = (CubeOpenShiftConfiguration) configuration;
        cubeOpenShiftConfiguration.setClient(client);
        openShiftAdapterProducer.set(new F8OpenShiftAdapter(client.getClient(), cubeOpenShiftConfiguration));
    }
}
