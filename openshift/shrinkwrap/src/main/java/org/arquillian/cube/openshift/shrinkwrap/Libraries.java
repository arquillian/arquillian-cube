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

import java.io.File;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Libraries {
    public static final PomStrategy MAVEN = new PomStrategy() {
        @Override
        public String[] profiles() {
            final String profiles = System.getProperty("cearq.maven.profiles");
            if (profiles == null) {
                return null;
            }
            return profiles.split(",");
        }

        @Override
        public String toPom() {
            return "pom.xml";
        }
    };

    public static File[] single(String groupId, String artifactId) {
        return single(MAVEN, groupId, artifactId);
    }

    public static File[] single(PomStrategy pomStrategy, String groupId, String artifactId) {
        return Maven.resolver().loadPomFromFile(pomStrategy.toPom(), pomStrategy.profiles()).resolve(groupId + ":" + artifactId).withoutTransitivity().asFile();
    }

    public static File[] transitive(String groupId, String artifactId) {
        return transitive(MAVEN, groupId, artifactId);
    }

    public static File[] transitive(PomStrategy pomStrategy, String groupId, String artifactId) {
        return Maven.resolver().loadPomFromFile(pomStrategy.toPom(), pomStrategy.profiles()).resolve(groupId + ":" + artifactId).withTransitivity().asFile();
    }
}
