package org.arquillian.cube.impl.client.enricher;

import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HostIpTestEnricher implements TestEnricher {

    @Inject
    Instance<HostIpContext> hostUriContext;

    @Override
    public void enrich(Object testCase) {
        if(hostUriContext.get() != null) {
            List<Field> fieldsWithAnnotation = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), HostIp.class);
            for (Field dockerHostIpField : fieldsWithAnnotation) {

                if (!dockerHostIpField.isAccessible()) {
                    dockerHostIpField.setAccessible(true);
                }

                if(String.class.isAssignableFrom(dockerHostIpField.getType())) {
                    try {
                        dockerHostIpField.set(testCase, hostUriContext.get().getHost());
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
        if (hostUriContext.get() != null) {
            Integer[] annotatedParameters = annotatedParameters(method);
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Integer i : annotatedParameters) {
                if (String.class.isAssignableFrom(parameterTypes[i])) {
                    values[i] = hostUriContext.get().getHost();
                }
            }
        }
        return values;
    }

    private Integer[] annotatedParameters(Method method) {
        List<Integer> parametersWithAnnotations = new ArrayList<>();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation a: paramAnnotations[i]) {
                if (a instanceof HostIp) {
                    parametersWithAnnotations.add(i);
                }
            }
        }
        return parametersWithAnnotations.toArray(new Integer[parametersWithAnnotations.size()]);
    }
}
