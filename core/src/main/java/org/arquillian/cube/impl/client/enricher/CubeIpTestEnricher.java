package org.arquillian.cube.impl.client.enricher;

import org.arquillian.cube.CubeIp;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CubeIpTestEnricher implements TestEnricher {

    private static final Logger logger = Logger.getLogger(CubeIpTestEnricher.class.getName());

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    @Override
    public void enrich(Object testCase) {
        if(cubeRegistryInstance.get() != null) {
            List<Field> fieldsWithAnnotation = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), CubeIp.class);
            for (Field dockerContainerIpField : fieldsWithAnnotation) {

                if (!dockerContainerIpField.isAccessible()) {
                    dockerContainerIpField.setAccessible(true);
                }

                if(String.class.isAssignableFrom(dockerContainerIpField.getType())) {
                    try {
                        final CubeIp cubeIpAnnotation = dockerContainerIpField.getAnnotation(CubeIp.class);
                        String containerName = cubeIpAnnotation.containerName();
                        boolean internal = cubeIpAnnotation.internal();
                        String ip = getContainerIp(containerName, internal);

                        if (ip != null) {
                            dockerContainerIpField.set(testCase, ip);
                        } else {
                            logger.log(Level.WARNING, String.format("There is no container with id %s.", containerName));
                        }
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        }
    }



    @Override
    public Object[] resolve(Method method) {
        Object[] values = new Object[method.getParameterTypes().length];
        if (cubeRegistryInstance.get() != null) {
            Integer[] annotatedParameters = annotatedParameters(method);
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Integer i : annotatedParameters) {
                if (String.class.isAssignableFrom(parameterTypes[i])) {
                    final Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                    final CubeIp cubeIpAnnotation = findCubeIp(parameterAnnotations);
                    String containerName = cubeIpAnnotation.containerName();
                    boolean internal = cubeIpAnnotation.internal();
                    String ip = getContainerIp(containerName, internal);
                    if (ip != null) {
                        values[i] = ip;
                    } else {
                        logger.log(Level.WARNING, String.format("There is no container with id %s.", containerName));
                    }
                }
            }
        }
        return values;
    }

    private CubeIp findCubeIp(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a instanceof CubeIp) {
                return (CubeIp)a;
            }
        }

        return null;
    }

    private Integer[] annotatedParameters(Method method) {
        List<Integer> parametersWithAnnotations = new ArrayList<>();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation a: paramAnnotations[i]) {
                if (a instanceof CubeIp) {
                    parametersWithAnnotations.add(i);
                }
            }
        }
        return parametersWithAnnotations.toArray(new Integer[parametersWithAnnotations.size()]);
    }

    private String getContainerIp(String containerName, boolean internal) {
        final Cube cube = getCube(containerName);

        if (cube == null) {
            return null;
        }

        if (cube.hasMetadata(HasPortBindings.class)) {
            final HasPortBindings metadata = (HasPortBindings) cube.getMetadata(HasPortBindings.class);
            if (internal) {
                return metadata.getInternalIP();
            } else {
                return metadata.getContainerIP();
            }
        } else {
            return null;
        }

    }


    private Cube getCube(String cubeId) {
        return cubeRegistryInstance.get().getCube(cubeId);
    }
}
