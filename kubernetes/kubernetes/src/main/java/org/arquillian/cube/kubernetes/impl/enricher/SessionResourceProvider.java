package org.arquillian.cube.kubernetes.impl.enricher;

import java.lang.annotation.Annotation;
import org.arquillian.cube.kubernetes.api.Session;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;

public class SessionResourceProvider extends AbstractKubernetesResourceProvider {

    @Inject
    private Instance<Session> sessionInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return Session.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        return getSession();
    }
}
