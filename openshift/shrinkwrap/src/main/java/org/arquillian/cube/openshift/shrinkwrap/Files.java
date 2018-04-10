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

package org.arquillian.cube.openshift.shrinkwrap;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ResourceContainer;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Files {
    public static void storeProperties(ResourceContainer container, Properties properties, String fileName) throws IOException {
        StringWriter writer = new StringWriter();
        properties.store(writer, "CE-Arquillian");
        container.addAsResource(new StringAsset(writer.toString()), fileName);
    }

    public static PropertiesHandle createPropertiesHandle(String fileName) {
        return new PropertiesHandle(fileName);
    }

    public static class PropertiesHandle {
        private final String fileName;
        private final Properties properties;

        private PropertiesHandle(String fileName) {
            this.fileName = fileName;
            this.properties = new Properties();
        }

        public void addProperty(String key, String value) {
            properties.setProperty(key, value);
        }

        public void store(ResourceContainer container) throws IOException {
            storeProperties(container, properties, fileName);
        }
    }
}
