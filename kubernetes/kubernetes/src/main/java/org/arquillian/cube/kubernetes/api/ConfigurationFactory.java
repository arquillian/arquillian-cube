package org.arquillian.cube.kubernetes.api;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;

public interface ConfigurationFactory<T extends Configuration> {

    T create(ArquillianDescriptor arquillian);

}