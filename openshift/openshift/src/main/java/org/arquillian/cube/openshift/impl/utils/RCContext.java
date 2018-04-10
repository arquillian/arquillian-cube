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

import java.util.List;
import java.util.Map;
import org.arquillian.cube.openshift.api.MountSecret;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class RCContext {
    private Archive<?> archive;
    private String imageName;
    private List<Port> ports;
    private Map<String, String> labels;
    private int replicas;
    private HookType lifecycleHook;
    private String preStopPath;
    private boolean ignorePreStop;
    private HookType probeHook;
    private List<String> probeCommands;
    private MountSecret mountSecret;

    public RCContext() {
    }

    public RCContext(Archive<?> archive, String imageName, List<Port> ports, Map<String, String> labels, int replicas, MountSecret mountSecret) {
        this.archive = archive;
        this.imageName = imageName;
        this.ports = ports;
        this.labels = labels;
        this.replicas = replicas;
        this.mountSecret = mountSecret;
    }

    public Archive<?> getArchive() {
        return archive;
    }

    public void setArchive(Archive<?> archive) {
        this.archive = archive;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public HookType getLifecycleHook() {
        return lifecycleHook;
    }

    public void setLifecycleHook(HookType lifecycleHook) {
        this.lifecycleHook = lifecycleHook;
    }

    public String getPreStopPath() {
        return preStopPath;
    }

    public void setPreStopPath(String preStopPath) {
        this.preStopPath = preStopPath;
    }

    public boolean isIgnorePreStop() {
        return ignorePreStop;
    }

    public void setIgnorePreStop(boolean ignorePreStop) {
        this.ignorePreStop = ignorePreStop;
    }

    public HookType getProbeHook() {
        return probeHook;
    }

    public void setProbeHook(HookType probeHook) {
        this.probeHook = probeHook;
    }

    public List<String> getProbeCommands() {
        return probeCommands;
    }

    public void setProbeCommands(List<String> probeCommands) {
        this.probeCommands = probeCommands;
    }

    public MountSecret getMountSecret() {
        return mountSecret;
    }

    public void setMountSecret(MountSecret mountSecret) {
        this.mountSecret = mountSecret;
    }
}
