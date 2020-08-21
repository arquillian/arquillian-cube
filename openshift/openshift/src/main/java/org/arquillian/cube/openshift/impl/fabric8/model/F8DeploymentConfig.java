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
package org.arquillian.cube.openshift.impl.fabric8.model;

import java.util.Map;
import org.arquillian.cube.openshift.api.model.DeploymentConfig;

/**
 * F8DeploymentConfig
 * <p/>
 * Wraps a F8 deployment config object.
 * 
 * @author Rob Cernich
 */
public class F8DeploymentConfig implements DeploymentConfig {

    private final io.fabric8.openshift.api.model.v4_10.DeploymentConfig delegate;

    /**
     * Create a new F8DeploymentConfig.
     */
    public F8DeploymentConfig(final io.fabric8.openshift.api.model.v4_10.DeploymentConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getMetadata().getName();
    }

    @Override
    public Integer getReplicas() {
        return delegate.getSpec().getReplicas();
    }

    @Override
    public Map<String, String> getSelector() {
        return delegate.getSpec().getSelector();
    }

    @Override
    public String toString() {
        return String.format("DeploymentConfig[name=%s,replicas=%s,selector=%s]", getName(), getReplicas(), getSelector());
    }

}
