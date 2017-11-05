package org.arquillian.cube.impl.client.enricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

/**
 * Implements Test enricher to get the binding port of a concrete exposed port of a cube.
 */
public class HostPortTestEnricher implements TestEnricher {

    private static final Logger logger = Logger.getLogger(HostPortTestEnricher.class.getName());

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    @Override
    public void enrich(Object testCase) {
        if (cubeRegistryInstance.get() != null) {
            List<Field> fieldsWithAnnotation =
                ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), HostPort.class);
            for (Field dockerHostPortField : fieldsWithAnnotation) {

                if (!dockerHostPortField.isAccessible()) {
                    dockerHostPortField.setAccessible(true);
                }

                if (int.class.isAssignableFrom(dockerHostPortField.getType()) || Integer.class.isAssignableFrom(
                    dockerHostPortField.getType())) {
                    try {
                        final HostPort hostPortAnnotation = dockerHostPortField.getAnnotation(HostPort.class);
                        String containerName = hostPortAnnotation.containerName();
                        int exposedPort = hostPortAnnotation.value();

                        int bindPort = getBindingPort(containerName, exposedPort);

                        if (bindPort > 0) {
                            dockerHostPortField.set(testCase, bindPort);
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
                if (int.class.isAssignableFrom(parameterTypes[i]) || Integer.class.isAssignableFrom(parameterTypes[i])) {
                    final Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                    final HostPort hostPortAnnotation = findHostPort(parameterAnnotations);
                    String containerName = hostPortAnnotation.containerName();
                    int exposedPort = hostPortAnnotation.value();

                    int bindPort = getBindingPort(containerName, exposedPort);
                    if (bindPort > 0) {
                        values[i] = bindPort;
                    } else {
                        logger.log(Level.WARNING, String.format("There is no container with id %s.", containerName));
                    }
                }
            }
        }
        return values;
    }

    private Integer[] annotatedParameters(Method method) {
        List<Integer> parametersWithAnnotations = new ArrayList<>();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation a : paramAnnotations[i]) {
                if (a instanceof HostPort) {
                    parametersWithAnnotations.add(i);
                }
            }
        }
        return parametersWithAnnotations.toArray(new Integer[parametersWithAnnotations.size()]);
    }

    private HostPort findHostPort(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a instanceof HostPort) {
                return (HostPort) a;
            }
        }

        return null;
    }

    private int getBindingPort(String cubeId, int exposedPort) {

        int bindPort = -1;

        final Cube cube = getCube(cubeId);

        if (cube != null) {
            final HasPortBindings portBindings = (HasPortBindings) cube.getMetadata(HasPortBindings.class);
            final HasPortBindings.PortAddress mappedAddress = portBindings.getMappedAddress(exposedPort);

            if (mappedAddress != null) {
                bindPort = mappedAddress.getPort();
            }
        }

        return bindPort;
    }

    private Cube getCube(String cubeId) {
        return cubeRegistryInstance.get().getCube(cubeId);
    }
}
