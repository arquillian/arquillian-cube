package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.client.KubernetesClient;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.impl.DefaultSession;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public abstract class AbstractKubernetesResourceProvider implements ResourceProvider {

    private static final String JAVAX_INJECT_NAMED = "javax.inject.Named";
    private static final String VALUE = "value";
    private static final Object[] NO_ARGS = new Object[0];

    @Inject
    private Instance<KubernetesClient> client;

    @Inject
    private Instance<DefaultSession> seesion;

    protected String getName(Annotation... qualifiers) {
        for (Annotation annotation : qualifiers) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (annotation instanceof Named) {
                return ((Named) annotation).value();
            } else if (type.getName().equals(JAVAX_INJECT_NAMED)) {
                return getAnnotationValue(annotation);
            }
        }
        return null;
    }

    /**
     * Get the value() from the specified annotation using reflection.
     *
     * @param annotation
     *     The annotation.
     */
    private String getAnnotationValue(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        try {
            Method method = type.getDeclaredMethod(VALUE);
            return String.valueOf(method.invoke(annotation, NO_ARGS));
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    protected KubernetesClient getClient() {
        return client.get();
    }

    public DefaultSession getSession() {
        return seesion.get();
    }
}
