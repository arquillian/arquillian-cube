package org.arquillian.cube.kubernetes.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar;

public class ExtensionRegistrar {

    private final ConfigurationRegistrar configurationRegistrar = new ConfigurationRegistrar();
    private final ArquillianDescriptor arquillian = configurationRegistrar.loadConfiguration();
    private Map<String, String> map = new HashMap<>();

    public DefaultConfiguration loadExtension(List<String> extension) {
        extension.forEach(ext -> map.putAll(arquillian.extension(ext).getExtensionProperties()));
        return DefaultConfiguration.fromMap(map);
    }

}
