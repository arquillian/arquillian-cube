package org.arquillian.cube.docker.drone;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.List;
import java.util.Map;

public class DroneOverwriteConfigurator {

    public void overwriteDroneConfiguration(@Observes ArquillianDescriptor arquillianDescriptor) {

        final List<ExtensionDef> extensions = arquillianDescriptor.getExtensions();

        final ExtensionDef webdriver = arquillianDescriptor.extension("webdriver");

        if (webdriver != null) {

            webdriver.property("remote", "true");

            final Map<String, String> extensionProperties = webdriver.getExtensionProperties();
            if (!extensionProperties.containsKey("browser")) {
                webdriver.property("browser", "firefox");
            }

        }
    }
}
