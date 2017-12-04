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

package org.arquillian.cube.openshift.api;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface OpenShiftHandle {
    String url(String podName, int port, String path, String parameters);

    InputStream execute(String podName, int port, String path) throws Exception;

    InputStream execute(int pod, int port, String path) throws Exception;

    InputStream execute(Map<String, String> labels, int pod, int port, String path) throws Exception;

    String exec(Map<String, String> labels, int waitSeconds, String... input) throws Exception;

    /**
     * Wait for ready pods.
     *
     * @param prefix   the RC prefix
     * @param replicas the expected size od ready pods
     * @throws Exception for any error
     */
    void waitForReadyPods(String prefix, int replicas) throws Exception;

    /**
     * Replace #size of pods via delete.
     *
     * @param prefix   the RC prefix
     * @param size the number of pods to replace
     * @param replicas the number of exepcted replicas after replace
     * @throws Exception for any error
     */
    void replacePods(String prefix, int size, int replicas) throws Exception;

    /**
     * Scale deployment to replicas.
     *
     * @param prefix   the RC prefix
     * @param replicas replicas
     * @throws Exception for any error
     */
    void scaleDeployment(String prefix, int replicas) throws Exception;

    /**
     * Get the logs for a given pod.
     *
     * @param podName the pod name
     * @return The pod's log
     * @throws Exception if a pod couldn't be found or if there's an error retrieving the log
     */
    String getLog(String podName) throws Exception;

    /**
     * Stream pod log into output stream.
     *
     * @param podName the pod name
     * @return log as a stream
     * @throws Exception for any error
     */
    InputStream streamLog(String podName) throws Exception;

    /**
     * Get the logs for a given pod.
     *
     * Combines both arguments to find a matching pod.
     *
     * @param  prefix Pod's name prefix, may be null
     * @param  labels The labels for selecting the pod, may be null
     * @return The pod's log
     * @throws Exception if a pod couldn't be found or if there's an error retrieving the log
     */
    String getLog(String prefix, Map<String, String> labels) throws Exception;

    /**
     * Get ready pods.
     *
     * @param prefix the prefix
     * @return ready pods
     * @throws Exception for any error
     */
    Set<String> getReadyPods(String prefix) throws Exception;

    /**
     * Get all pods.
     *
     * @return all pods
     * @throws Exception for any error
     */
    List<String> getPods() throws Exception;

    /**
     * Get all pods.
     *
     * @param prefix RC name, if null all pods are returned
     * @return all pods
     * @throws Exception for any error
     */
    List<String> getPods(String prefix) throws Exception;

    /**
     * Delete pod by name.
     *
     * @param podName pod name
     * @param gracePeriodSeconds grace period, -1 if none / default
     * @throws Exception for any error
     */
    void deletePod(String podName, long gracePeriodSeconds) throws Exception;

    /**
     * Trigger deployment config update. A dummy variable is inserted into
     * deployment config in order to trigger the redeployment.
     *
     * @param prefix the deployment config prefix
     * @param wait   wait until update is done
     * @throws Exception for any error
     */
    void triggerDeploymentConfigUpdate(String prefix, boolean wait) throws Exception;

    /**
     * Trigger deployment config update
     *
     * @param prefix    the deployment config prefix
     * @param wait      wait until update is done
     * @param variables Environment variables to insert into the deployment. If null
     *                  or empty, a dummy variable is inserted in order to trigger
     *                  the redeployment.
     * @throws Exception for any error
     */
    void triggerDeploymentConfigUpdate(String prefix, boolean wait, Map<String, String> variables) throws Exception;

    // Jolokia support

    /**
     * Input is on purpose plain Object.
     */
    <T> T jolokia(Class<T> expectedReturnType, String podName, Object input) throws Exception;
}
