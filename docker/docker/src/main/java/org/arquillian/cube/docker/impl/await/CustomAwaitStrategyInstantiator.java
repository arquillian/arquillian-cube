package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomAwaitStrategyInstantiator implements AwaitStrategy {

    private Cube<?> cube;
    private DockerClientExecutor dockerClientExecutor;
    private Await params;

    public CustomAwaitStrategyInstantiator(Cube<?> cube, DockerClientExecutor dockerClientExecutor, Await params) {
        this.cube = cube;
        this.dockerClientExecutor = dockerClientExecutor;
        this.params = params;
    }

    @Override
    public boolean await() {

        String className = this.params.getStrategy();
        try {
            Class<? extends AwaitStrategy> customStrategy = (Class<? extends AwaitStrategy>) Class.forName(className);
            AwaitStrategy customStrategyInstance = customStrategy.newInstance();

            // Inject if there is a setter for Cube, DockerClientExecutor or Await
            for(PropertyDescriptor propertyDescriptor :
                    Introspector.getBeanInfo(customStrategyInstance.getClass()).getPropertyDescriptors()){
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod != null) {
                    if (writeMethod.getParameterTypes()[0].isAssignableFrom(Cube.class)) {
                        writeMethod.invoke(customStrategyInstance, this.cube);
                    } else {
                        if (writeMethod.getParameterTypes()[0].isAssignableFrom(DockerClientExecutor.class)) {
                            writeMethod.invoke(customStrategyInstance, this.dockerClientExecutor);
                        } else {
                            if (writeMethod.getParameterTypes()[0].isAssignableFrom(Await.class)) {
                                writeMethod.invoke(customStrategyInstance, this.params);
                            }
                        }
                    }
                }
            }

            // Finally we call the await method
            return customStrategyInstance.await();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IntrospectionException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
