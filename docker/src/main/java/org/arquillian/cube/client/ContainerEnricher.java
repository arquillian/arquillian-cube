package org.arquillian.cube.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.arquillian.cube.Container;
import org.arquillian.cube.util.ReflectionUtil;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

public class ContainerEnricher implements TestEnricher {

    @Inject
    private Instance<ContainerMapping> containerMappingInstance;

    @Override
    public void enrich(Object testCase) {
        
        List<Field> testFields = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), Container.class);

        for (Field testField : testFields) {
            try {
                if (String.class.isAssignableFrom(testField.getType())) {
                    if (!testField.isAccessible()) {
                        testField.setAccessible(true);
                    }

                    Container containerAnnotation = testField.getAnnotation(Container.class);
                    ContainerMapping containerMapping = this.containerMappingInstance.get();
                    if(containerAnnotation.name() != null && !"".equals(containerAnnotation.name().trim())) {
                        testField.set(testCase, containerMapping.getContainerByName(containerAnnotation.name()));
                    } else {
                        testField.set(testCase, containerMapping.getDefaultContainer());
                    }
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
