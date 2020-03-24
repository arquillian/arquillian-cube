package org.arquillian.cube.docker.impl.afterStart;

import org.arquillian.cube.docker.impl.client.config.CustomAfterStartAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.CubeId;
import org.arquillian.cube.spi.afterStart.AfterStartAction;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomAfterStartActionInstantiator implements AfterStartAction {
    private CubeId containerId;
    private DockerClientExecutor dockerClientExecutor;
    private CustomAfterStartAction customAfterStartAction;

    public CustomAfterStartActionInstantiator(CubeId containerId, DockerClientExecutor dockerClientExecutor,
                                              CustomAfterStartAction customAfterStartAction) {
        this.containerId = containerId;
        this.dockerClientExecutor = dockerClientExecutor;
        this.customAfterStartAction = customAfterStartAction;
    }

    @Override
    public void doAfterStart() {
        try {
            String classname = customAfterStartAction.getStrategy();
            Class<? extends AfterStartAction> customStrategy =
                (Class<? extends AfterStartAction>) Class.forName(classname);
            AfterStartAction customStrategyInstance = customStrategy.newInstance();

            // Inject if there is a field of type Cube, DockerClientExecutor
            final Field[] fields = customStrategyInstance.getClass().getDeclaredFields();
            for (Field field : fields) { // Inject if there is a field of type Cube, DockerClientExecutor
                if (field.getType().isAssignableFrom(DockerClientExecutor.class)) {
                    field.setAccessible(true);
                    field.set(customStrategyInstance, this.dockerClientExecutor);
                } else if (field.getType().isAssignableFrom(CubeId.class)) {
                    field.setAccessible(true);
                    field.set(customStrategyInstance, this.containerId);
                }
            }

            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(customStrategyInstance.getClass())
                .getPropertyDescriptors()) {
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod != null) {
                    if (writeMethod.getParameterTypes()[0].isAssignableFrom(CubeId.class)) {
                        writeMethod.invoke(customStrategyInstance, this.containerId);
                    } else if (writeMethod.getParameterTypes()[0].isAssignableFrom(DockerClientExecutor.class)) {
                        writeMethod.invoke(customStrategyInstance, this.dockerClientExecutor);
                    }
                }
            }

            // Finally, we call the afterStart method
            customStrategyInstance.doAfterStart();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | IntrospectionException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
