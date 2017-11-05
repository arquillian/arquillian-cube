package org.arquillian.cube.impl.client.container;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.arquillian.cube.impl.util.ContainerUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.HasPortBindings.PortAddress;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.core.api.annotation.Observes;

public class ContainerConfigurationController {

    private static final Pattern portPattern = Pattern.compile("(?i:.*port.*)");
    //private static final Pattern hostPattern = Pattern.compile("(?i:.*host.*)");
    private static final Pattern addressPattern = Pattern.compile("(?i:.*address.*)");
    private PortAddress mappingForPort = null;
    //private static final Pattern jmxPattern = Pattern.compile("(?i:.*jmx.*)");

    public void remapContainer(@Observes BeforeSetup event, CubeRegistry cubeRegistry,
        ContainerRegistry containerRegistry) throws InstantiationException, IllegalAccessException {

        Container container = ContainerUtil.getContainerByDeployableContainer(containerRegistry,
            event.getDeployableContainer());
        if (container == null) {
            return;
        }

        Cube<?> cube = cubeRegistry.getCube(ContainerUtil.getCubeIDForContainer(container));
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }

        HasPortBindings bindings = cube.getMetadata(HasPortBindings.class);
        if (bindings == null) {
            return;
        }

        ContainerDef containerConfiguration = container.getContainerConfiguration();
        //Get the port property
        List<String> portPropertiesFromArquillianConfigurationFile =
            filterArquillianConfigurationPropertiesByPortAttribute(containerConfiguration);
        //Get the AddressProperty property
        List<String> addressPropertiesFromArquillianConfigurationFile =
            filterArquillianConfigurationPropertiesByAddressAttribute(containerConfiguration);

        Class<?> configurationClass = container.getDeployableContainer().getConfigurationClass();
        //Get the port property
        List<PropertyDescriptor> configurationClassPortFields =
            filterConfigurationClassPropertiesByPortAttribute(configurationClass);
        //Get the Address property
        List<PropertyDescriptor> configurationClassAddressFields =
            filterConfigurationClassPropertiesByAddressAttribute(configurationClass);

        Object newConfigurationInstance = configurationClass.newInstance();

        for (PropertyDescriptor configurationClassPortField : configurationClassPortFields) {
            int containerPort = getDefaultPortFromConfigurationInstance(newConfigurationInstance,
                configurationClass, configurationClassPortField);
            mappingForPort = bindings.getMappedAddress(containerPort);

            if (!portPropertiesFromArquillianConfigurationFile.contains(configurationClassPortField.getName()) && (
                configurationClassPortField.getPropertyType().equals(Integer.class)
                    || configurationClassPortField.getPropertyType().equals(int.class))) {
                // This means that port has not configured in arquillian.xml and it will use default value.
                // In this case is when remapping should be activated to adequate the situation according to
                // Arquillian Cube exposed ports.
                if (mappingForPort != null) {
                    containerConfiguration.overrideProperty(configurationClassPortField.getName(),
                        Integer.toString(mappingForPort.getPort()));
                }
            }
        }

        for (PropertyDescriptor configurationClassAddressField : configurationClassAddressFields) {
            if (!addressPropertiesFromArquillianConfigurationFile.contains(configurationClassAddressField.getName()) && (
                configurationClassAddressField.getPropertyType().equals(String.class)
                    || configurationClassAddressField.getPropertyType().equals(String.class))) {
                // If a property called portForwardBindAddress on openshift qualifier on arquillian.xml exists it will overrides the
                // arquillian default|defined managementAddress with the IP address given on this property.
                if (mappingForPort != null) {
                    containerConfiguration.overrideProperty(configurationClassAddressField.getName(),
                        mappingForPort.getIP());
                }
            }
        }
    }

    private int getDefaultPortFromConfigurationInstance(Object configurationInstance, Class<?> configurationClass,
        PropertyDescriptor fieldName) {

        try {
            Method method = fieldName.getReadMethod();
            if (method == null) {
                return -1;
            }
            return (int) method.invoke(configurationInstance);
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<String> filterArquillianConfigurationPropertiesByPortAttribute(ContainerDef containerDef) {
        List<String> fields = new ArrayList<String>();

        for (Entry<String, String> entry : containerDef.getContainerProperties().entrySet()) {
            if (portPattern.matcher(entry.getKey()).matches()) {
                fields.add(entry.getKey());
            }
        }

        return fields;
    }

    private List<String> filterArquillianConfigurationPropertiesByAddressAttribute(ContainerDef containerDef) {
        List<String> fields = new ArrayList<String>();

        for (Entry<String, String> entry : containerDef.getContainerProperties().entrySet()) {
            if (addressPattern.matcher(entry.getKey()).matches()) {
                fields.add(entry.getKey());
            }
        }

        return fields;
    }

    private List<PropertyDescriptor> filterConfigurationClassPropertiesByPortAttribute(Class<?> configurationClass) {

        List<PropertyDescriptor> fields = new ArrayList<PropertyDescriptor>();

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(configurationClass, Object.class)
                .getPropertyDescriptors();

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String propertyName = propertyDescriptor.getName();

                if (portPattern.matcher(propertyName).matches()) {
                    if (int.class.equals(propertyDescriptor.getPropertyType()) || Integer.class.equals(
                        propertyDescriptor.getPropertyType())) {
                        fields.add(propertyDescriptor);
                    }
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        return fields;
    }

    private List<PropertyDescriptor> filterConfigurationClassPropertiesByAddressAttribute(Class<?> configurationClass) {

        List<PropertyDescriptor> fields = new ArrayList<PropertyDescriptor>();

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(configurationClass, Object.class)
                .getPropertyDescriptors();

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String propertyName = propertyDescriptor.getName();

                if (addressPattern.matcher(propertyName).matches()) {
                    fields.add(propertyDescriptor);
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        return fields;
    }
}
