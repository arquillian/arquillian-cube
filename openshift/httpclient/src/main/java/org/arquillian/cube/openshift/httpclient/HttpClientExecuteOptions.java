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

package org.arquillian.cube.openshift.httpclient;

/**
 * Options for use with HttpClient.Execute() method. Use the
 * HttpClientExecuteOptions.Builder to create an instance.
 *
 * @author Jonh Wendell
 */
public class HttpClientExecuteOptions {
    private final int tries;
    private final int delay;
    private final int desiredStatusCode;

    private HttpClientExecuteOptions(Builder b) {
        tries = b.tries;
        delay = b.delay;
        desiredStatusCode = b.desiredStatusCode;
    }

    public int getTries() {
        return tries;
    }

    public int getDelay() {
        return delay;
    }

    public int getDesiredStatusCode() {
        return desiredStatusCode;
    }

    public static class Builder {
        private int tries = 1;
        private int delay = 5;
        private int desiredStatusCode = -1;

        /**
         * How many tries should we do before giving up. Default value is 1.
         *
         * @param value number of tries
         * @return this
         */
        public Builder tries(int value) {
            tries = value;
            return this;
        }

        /**
         * Delay, in seconds, between tries. Default value is 5.
         *
         * @param value delay
         * @return this
         */
        public Builder delay(int value) {
            delay = value;
            return this;
        }

        /**
         * If set, the response status code must match this value. If it
         * doesn't, another try is made (see {@link #tries(int)}). Default value
         * is -1, meaning we should not compare response codes.
         *
         * @param value desired status code
         * @return this
         */
        public Builder desiredStatusCode(int value) {
            desiredStatusCode = value;
            return this;
        }

        public HttpClientExecuteOptions build() {
            return new HttpClientExecuteOptions(this);
        }
    }
}
