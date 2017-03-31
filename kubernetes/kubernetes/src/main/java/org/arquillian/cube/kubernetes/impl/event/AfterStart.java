package org.arquillian.cube.kubernetes.impl.event;

import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.SessionCreatedEvent;


public class AfterStart extends SessionCreatedEvent{

    public AfterStart(Session session) {
        super(session);
    }
}
