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

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.cube.kubernetes.impl.event.Stop;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class SuiteListener {

    @Inject
    @ApplicationScoped
    private InstanceProducer<DefaultSession> sessionProducer;

    @Inject
    private Event<SessionCreatedEvent> controlEvent;

    private DefaultSession session;

    public void start(@Observes(precedence = 100) BeforeSuite event, Configuration configuration, Logger logger) {
        session = new DefaultSession(configuration.getSessionId(), configuration.getNamespace(), logger);
        session.init();
        sessionProducer.set(session);
        controlEvent.fire(new Start(session));
    }

    public void stop(@Observes(precedence = -100) AfterSuite event, Logger logger) {
        controlEvent.fire(new Stop(session));
        session.destroy();
    }
}
