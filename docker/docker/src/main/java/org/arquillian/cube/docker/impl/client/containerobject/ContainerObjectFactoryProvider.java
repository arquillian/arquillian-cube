package org.arquillian.cube.docker.impl.client.containerobject;

import java.lang.annotation.Annotation;
import java.util.Optional;
import org.arquillian.cube.ContainerObjectFactory;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class ContainerObjectFactoryProvider implements ResourceProvider {

    @Inject
    private Instance<ContainerObjectFactory> instance;

    @Override
    public boolean canProvide(Class<?> type) {
        return ContainerObjectFactory.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return Optional.ofNullable(instance.get())
            .orElseThrow(() -> new IllegalStateException(
                String.format("%s was not found.", ContainerObjectFactory.class.getSimpleName())));
    }
}
