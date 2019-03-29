package org.arquillian.cube.impl.client.observer;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.util.Collection;

public class BeforeClassEnricherObserver {

    @Inject
    Instance<ServiceLoader> serviceLoaderInstance;

    public void executeEnrichers(@Observes BeforeClass beforeClass) throws Exception {

        if (null != serviceLoaderInstance) {
            final Collection<TestEnricher> all = serviceLoaderInstance.get().all(TestEnricher.class);
            final Class<?> javaClass = beforeClass.getTestClass().getJavaClass();

            for (final TestEnricher testEnricher : all) {
                testEnricher.enrich(javaClass.newInstance());
            }
        }
    }
}
