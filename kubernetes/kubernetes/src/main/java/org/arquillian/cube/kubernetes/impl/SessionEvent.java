/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import org.arquillian.cube.kubernetes.api.Session;
import org.jboss.arquillian.core.spi.event.Event;

public class SessionEvent implements Event {

    private Session session;

    public SessionEvent(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }
}
