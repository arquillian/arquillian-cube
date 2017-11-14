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

package org.arquillian.cube.openshift.impl.ext;

import org.arquillian.cube.openshift.api.ConfigurationHandle;
import org.arquillian.cube.openshift.api.OpenShiftHandle;
import org.arquillian.cube.openshift.api.Tools;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class UtilsArchiveAppender implements AuxiliaryArchiveAppender {
    @Inject
    private Instance<ConfigurationHandle> configurationInstance;

    public Archive<?> createAuxiliaryArchive() {
        return ShrinkWrap.create(JavaArchive.class, "ce-arq-utils.jar")
            .add(new StringAsset(RemoteConfigurationResourceProvider.toProperties(configurationInstance.get())), RemoteConfigurationResourceProvider.FILE_NAME)
            .addClass(ConfigurationHandle.class)
            .addClass(OpenShiftHandle.class)
            .addClass(Tools.class)
            .addClass(UtilsCEExtensionContainer.class)
            .addClass(RemoteConfigurationResourceProvider.class)
            .addAsServiceProviderAndClasses(RemoteLoadableExtension.class, UtilsCEExtensionContainer.class);
    }

}
