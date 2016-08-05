package org.arquillian.cube.kubernetes.impl.resolve;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

/**
 * Created by iocanel on 8/5/16.
 */
public class DependencyResolverRegistar {

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Inject @ApplicationScoped
    InstanceProducer<DependencyResolver> dependencyResolver;

    public void install(@Observes(precedence = 100) Configuration configuration) {
        dependencyResolver.set(serviceLoader.get().onlyOne(DependencyResolver.class, ShrinkwrapResolver.class));

    }
}
