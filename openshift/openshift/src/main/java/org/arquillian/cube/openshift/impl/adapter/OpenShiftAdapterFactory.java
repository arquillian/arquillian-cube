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

import io.fabric8.openshift.clnt.v4_10.NamespacedOpenShiftClient;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class OpenShiftAdapterFactory {
    public static OpenShiftAdapter getOpenShiftAdapter(NamespacedOpenShiftClient client, CubeOpenShiftConfiguration configuration) {
        ServiceLoader<OpenShiftAdapterProvider> adapters = ServiceLoader.load(OpenShiftAdapterProvider.class, OpenShiftAdapterFactory.class.getClassLoader());
        //noinspection LoopStatementThatDoesntLoop
        for (OpenShiftAdapterProvider pp : adapters) {
            Logger.getLogger(OpenShiftAdapterFactory.class.getName()).info(String.format("Using %s to access OpenShift API ...", pp.getClass().getSimpleName()));
            return pp.create(client, configuration);
        }
        throw new IllegalStateException("No OpenShiftAdapterProvider found!");
    }
}
