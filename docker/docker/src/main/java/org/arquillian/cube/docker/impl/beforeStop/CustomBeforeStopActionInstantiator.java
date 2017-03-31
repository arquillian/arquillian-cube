package org.arquillian.cube.docker.impl.beforeStop;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.beforeStop.BeforeStopStrategy;

public class CustomBeforeStopActionInstantiator implements BeforeStopStrategy {
    private String containerId;
    private DockerClientExecutor dockerClientExecutor;
    private String className;

    public CustomBeforeStopActionInstantiator(String containerId, DockerClientExecutor dockerClientExecutor, String className) {
        this.containerId = containerId;
        this.dockerClientExecutor = dockerClientExecutor;
        this.className = className;
    }

    @Override
    public void doBeforeStop() {

        try {

            Class<? extends BeforeStopStrategy> customStrategy = (Class<? extends BeforeStopStrategy>) Class.forName(className);
            BeforeStopStrategy customStrategyInstance = customStrategy.newInstance();

            // Inject if there is a field of type Cube, DockerClientExecutor
            final Field[] fields = customStrategyInstance.getClass().getDeclaredFields();
            for (Field field : fields) { // Inject if there is a field of type Cube, DockerClientExecutor
                if (field.getType().isAssignableFrom(DockerClientExecutor.class)) {
                    field.setAccessible(true);
                    field.set(customStrategyInstance, this.dockerClientExecutor);
                }
            }

            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(customStrategyInstance.getClass()).getPropertyDescriptors()) {
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod != null) {
                    if (writeMethod.getParameterTypes()[0].isAssignableFrom(String.class)) {
                        writeMethod.invoke(customStrategyInstance, this.containerId);
                    } else if (writeMethod.getParameterTypes()[0].isAssignableFrom(DockerClientExecutor.class)) {
                        writeMethod.invoke(customStrategyInstance, this.dockerClientExecutor);
                    }
                }
            }

            // Finally, we call the beforeStop method
            customStrategyInstance.doBeforeStop();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | IntrospectionException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
