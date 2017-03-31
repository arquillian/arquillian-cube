package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.impl.util.ReflectionUtil;

public class AutoStartOrderFactory {

    public static DockerAutoStartOrder createDefaultDockerAutoStartOrder() {
        return new DefaultDockerAutoStartOrder();
    }

    public static DockerAutoStartOrder createDockerAutoStartOrder(String clazz) {
        if (ReflectionUtil.isClassPresent(clazz)) {
            return ReflectionUtil.newInstance(clazz, new Class[0], new Object[0], DockerAutoStartOrder.class);
        } else {
            throw new IllegalArgumentException(String.format("Class %s is not found in classpath.", clazz));
        }
    }
}
