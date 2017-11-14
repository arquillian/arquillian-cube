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

import java.util.logging.Logger;

/**
 * Parallel handle.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ParallelHandle {
    private static final Logger log = Logger.getLogger(ParallelHandle.class.getName());

    private enum State {
        INIT,
        DONE,
        IN_PROGRESS,
        WAITING,
        ERROR
    }

    private volatile State state = State.INIT;
    private Throwable error;
    private int counter;

    Throwable getError() {
        return error;
    }

    synchronized void init(String info) {
        log.info(String.format("Build %s init [%s] (%s)", info, state, counter));
        if (state == State.INIT) {
            state = State.IN_PROGRESS;
        }
        counter += 2; // we need both, Main and RunInPod to clear
    }

    synchronized void clear(String info, String from) {
        log.info(String.format("Clear build %s from %s [%s] (%s)", info, from, state, counter));
        if (state == State.WAITING) {
            notifyAll(); // just to make sure, we don't somehow hang
        }
        counter--;
        if (counter == 0) {
            log.info(String.format("Reset build %s [%s]", info, state));
            state = State.INIT;
        }
    }

    synchronized void doNotify(String info) {
        if (state == State.WAITING) {
            log.info(String.format("Notifying builds waiting on %s ...", info));
            notifyAll();
        } else {
            log.info(String.format("Build %s not waiting [%s].", info, state));
        }
        state = State.DONE;
    }

    synchronized void doError(String info, Throwable error) {
        log.warning(String.format("Error in %s build: %s [%s]", info, error, state));
        this.error = error;
        if (state == State.WAITING) {
            notifyAll();
        }
        state = State.ERROR;
    }

    synchronized void doWait(String info) {
        if (state == State.IN_PROGRESS) {
            log.info(String.format("Waiting for %s build to finish ...", info));
            state = State.WAITING;
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        } else {
            log.info(String.format("Build %s not in-progress [%s]", info, state));
        }
    }
}
