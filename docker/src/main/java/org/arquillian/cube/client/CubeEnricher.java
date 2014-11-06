package org.arquillian.cube.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.arquillian.cube.Cube;
import org.arquillian.cube.docker.DockerClientExecutor;
import org.arquillian.cube.util.ReflectionUtil;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

import com.github.dockerjava.api.DockerClient;

public class CubeEnricher implements TestEnricher {

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutor;

    @Override
    public void enrich(Object testCase) {
        
        List<Field> testFields = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), Cube.class);

        for (Field testField : testFields) {
            try {
                if (DockerClient.class.isAssignableFrom(testField.getType())) {
                    if (!testField.isAccessible()) {
                        testField.setAccessible(true);
                    }

                    DockerClientExecutor dockerClientExecutor = this.dockerClientExecutor.get();
                    testField.set(testCase, dockerClientExecutor.getDockerClient());
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not inject mocked object on field " + testField, e);
            }
        }

    }

    @Override
    public Object[] resolve(Method method) {
        return new Object[method.getParameterTypes().length];
    }

}
