package org.arquillian.cube.docker.impl.client.container;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.util.ContainerUtil;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class DockerServerIPConfigurator {

    //private static final Pattern portPattern = Pattern.compile("(?i:.*port.*)");
    private static final Pattern hostPattern = Pattern.compile("(?i:.*host.*)");
    private static final Pattern addressPattern = Pattern.compile("(?i:.*address.*)");
    private static final Pattern jmxPattern = Pattern.compile("(?i:.*jmx.*)");

    @Inject
    private Instance<OperatingSystemFamily> familyInstance;

    public void applyDockerServerIpChange(@Observes BeforeSetup event, CubeRegistry cubeRegistry,
        ContainerRegistry containerRegistry)
        throws InstantiationException, IllegalAccessException, MalformedURLException {

        Container container = ContainerUtil.getContainerByDeployableContainer(containerRegistry,
            event.getDeployableContainer());
        if (container == null) {
            return;
        }
        Cube<?> cube = cubeRegistry.getCube(org.arquillian.cube.impl.util.ContainerUtil.getCubeIDForContainer(container));
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }

        HasPortBindings portBindings = cube.getMetadata(HasPortBindings.class);
        if (portBindings == null) {
            return;
        }

        ContainerDef containerConfiguration = container.getContainerConfiguration();
        boolean foundAttribute =
            resolveConfigurationPropertiesWithDockerServerIp(containerConfiguration, portBindings.getContainerIP());

        //if user doesn't configured in arquillian.xml the host then we can override the default value.
        if (!foundAttribute) {
            Class<?> configurationClass = container.getDeployableContainer().getConfigurationClass();
            List<PropertyDescriptor> configurationClassHostOrAddressFields =
                filterConfigurationClassPropertiesByHostOrAddressAttribute(configurationClass);
            for (PropertyDescriptor propertyDescriptor : configurationClassHostOrAddressFields) {
                //we get default address value and we replace to boot2docker ip
                containerConfiguration.overrideProperty(propertyDescriptor.getName(), portBindings.getContainerIP());
            }
        }
    }

    private boolean resolveConfigurationPropertiesWithDockerServerIp(ContainerDef containerDef, String containerIP) {

        boolean foundAttribute = false;
        for (Entry<String, String> entry : containerDef.getContainerProperties().entrySet()) {
            if ((hostPattern.matcher(entry.getKey()).matches() || addressPattern.matcher(entry.getKey()).matches())) {
                //if property is already configured, doesn't matter if it is a boot2docker or not we can say that we have matched a defined property.
                foundAttribute = true;
                if (entry.getValue().contains(CubeDockerConfiguration.DOCKER_SERVER_IP)) {
                    containerDef.overrideProperty(entry.getKey(),
                        entry.getValue().replaceAll(CubeDockerConfiguration.DOCKER_SERVER_IP, containerIP));
                }
            }
        }
        return foundAttribute;
    }

    private List<PropertyDescriptor> filterConfigurationClassPropertiesByHostOrAddressAttribute(
        Class<?> configurationClass) {

        List<PropertyDescriptor> fields = new ArrayList<PropertyDescriptor>();

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(configurationClass, Object.class)
                .getPropertyDescriptors();

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String propertyName = propertyDescriptor.getName();

                if ((hostPattern.matcher(propertyName).matches() || addressPattern.matcher(propertyName).matches())
                    && (!jmxPattern.matcher(propertyName).matches())) {
                    fields.add(propertyDescriptor);
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        return fields;
    }
}
