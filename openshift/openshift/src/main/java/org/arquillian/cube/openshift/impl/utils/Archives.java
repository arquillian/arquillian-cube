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

package org.arquillian.cube.openshift.impl.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javassist.util.proxy.MethodHandler;
import org.arquillian.cube.openshift.api.ExternalDeployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Archives {
    public static final String MGMT_CLIENT_JAR_NAME = "incontainermgmtclient.jar";

    private final static String WEB_XML =
        "<web-app version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
            "         metadata-complete=\"false\">\n" +
            "</web-app>";

    private static final String[] defaultDependencies = {
        "org.jboss.as.server",
        "org.jboss.as.controller-client",
        "org.jboss.jandex",
        "org.jboss.logging",
        "org.jboss.modules",
        "org.jboss.dmr",
        "org.jboss.msc"
    };

    public static boolean isMgmtClientJar(Archive<?> archive) {
        return MGMT_CLIENT_JAR_NAME.equals(archive.getName());
    }

    public static boolean isExternalDeployment(Class<?> clazz) {
        return ReflectionUtils.isAnnotationPresent(clazz, ExternalDeployment.class);
    }

    public static WebArchive generateDummyWebArchive() {
        return generateDummyWebArchive(null);
    }

    public static WebArchive generateDummyWebArchive(String name) {
        WebArchive war = (name == null) ? ShrinkWrap.create(WebArchive.class) : ShrinkWrap.create(WebArchive.class, name);
        war.setWebXML(new StringAsset(WEB_XML));
        return war;
    }

    public static DeploymentDescription generateDummyDeployment(String name) {
        return new DeploymentDescription("_DEFAULT_", generateDummyWebArchive(name));
    }

    public static Archive<?> toProxy(final Archive<?> archive, final String newArchiveName) {
        Class<? extends Archive> expected = (archive instanceof EnterpriseArchive) ? EnterpriseArchive.class : WebArchive.class;
        return BytecodeUtils.proxy(expected, new MethodHandler() {
            public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
                if ("getName".equals(method.getName())) {
                    return newArchiveName;
                } else {
                    return method.invoke(archive, args);
                }
            }
        });
    }

    public static void handleDependencies(Archive<?> archive) {
        if (archive instanceof ManifestContainer<?> == false) {
            throw new IllegalArgumentException("ManifestContainer expected: " + archive);
        }

        final Manifest manifest = getOrCreateManifest(archive);
        ManifestContainer manifestContainer = ManifestContainer.class.cast(archive);

        Attributes attributes = manifest.getMainAttributes();
        if (attributes.getValue(Attributes.Name.MANIFEST_VERSION.toString()) == null) {
            attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        }
        String value = attributes.getValue("Dependencies");
        StringBuilder moduleDeps = new StringBuilder(value != null && value.trim().length() > 0 ? value : "");
        moduleDeps.append(defaultDependencies[0]);
        for (int i = 1; i < defaultDependencies.length; i++) {
            moduleDeps.append(",").append(defaultDependencies[i]);
        }

        attributes.putValue("Dependencies", moduleDeps.toString());

        // Add the manifest to the archive
        ArchivePath manifestPath = ArchivePaths.create(JarFile.MANIFEST_NAME);
        archive.delete(manifestPath);
        manifestContainer.addAsManifestResource(new Asset() {
            public InputStream openStream() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    manifest.write(baos);
                    return new ByteArrayInputStream(baos.toByteArray());
                } catch (IOException ex) {
                    throw new IllegalStateException("Cannot write manifest", ex);
                }
            }
        }, "MANIFEST.MF");
    }

    private static Manifest getOrCreateManifest(Archive<?> archive) {
        Manifest manifest;
        try {
            Node node = archive.get(JarFile.MANIFEST_NAME);
            if (node == null) {
                manifest = new Manifest();
                Attributes attributes = manifest.getMainAttributes();
                attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            } else {
                manifest = new Manifest(node.getAsset().openStream());
            }
            return manifest;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot obtain manifest", ex);
        }
    }
}
