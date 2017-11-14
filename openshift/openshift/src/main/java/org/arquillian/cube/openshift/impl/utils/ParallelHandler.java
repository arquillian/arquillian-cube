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

/**
 * Parallel handle.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ParallelHandler {
    private volatile ParallelHandle main = new ParallelHandle();
    private volatile ParallelHandle spi = new ParallelHandle();

    void initMain() {
        main.init("Main");
    }

    void resumeOnMain() {
        main.doNotify("Main");
    }

    void errorInMain(Throwable error) {
        main.doError("Main", error);
    }

    void waitOnMain() {
        main.doWait("Main");
    }

    Throwable getErrorFromMain() {
        return main.getError();
    }

    void clearMain() {
        main.clear("Main", "Main");
        spi.clear("RunInPod", "Main");
    }

    void initSPI() {
        spi.init("RunInPod");
    }

    void resumeOnSPI() {
        spi.doNotify("RunInPod");
    }

    public void errorInSPI(Throwable error) {
        spi.doError("RunInPod", error);
    }

    void waitOnSPI() {
        spi.doWait("RunInPod");
    }

    Throwable getErrorFromSPI() {
        return spi.getError();
    }

    void clearSPI() {
        spi.clear("RunInPod", "RunInPod");
        main.clear("Main", "RunInPod");
    }
}
