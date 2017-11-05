package org.arquillian.cube.kubernetes.impl.enricher;

import com.fasterxml.jackson.databind.DeserializationFeature;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.utils.Serialization;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.WithLabel;
import org.arquillian.cube.kubernetes.annotations.WithLabels;
import org.arquillian.cube.kubernetes.impl.DefaultSession;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public abstract class AbstractKubernetesResourceProvider implements ResourceProvider {

    private static final String JAVAX_INJECT_NAMED = "javax.inject.Named";
    private static final String VALUE = "value";
    private static final Object[] NO_ARGS = new Object[0];

    private static final String VERSION_PACKAGE_PATTERN = ".v\\d+_\\d+";
    private static final String CLNT = "clnt";
    private static final String CLIENT = "client";

    @Inject
    private Instance<KubernetesClient> client;

    @Inject
    private Instance<DefaultSession> session;

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

    protected Map<String, String> getLabels(Annotation... qualifiers) {
        HashMap<String, String> rc = new HashMap<>();
        for (Annotation annotation : qualifiers) {
            if (annotation instanceof WithLabel) {
                WithLabel l = (WithLabel) annotation;
                rc.put(l.name(), l.value());
            } else if (annotation instanceof WithLabels) {
                WithLabels ls = (WithLabels) annotation;
                for (WithLabel l : ls.value()) {
                    rc.put(l.name(), l.value());
                }
            }
        }
        return rc;
    }


    protected String internalToUserType(String fqn) {
       return fqn.replaceAll(VERSION_PACKAGE_PATTERN, "").replaceAll(CLNT, CLIENT);
    }

    protected <I,O> O toUsersResource(I input) {
        return toUsersResource(input, internalToUserType(input.getClass().getCanonicalName()));
    }

    protected <I,O> O toUsersResource(I input, String className) {
        if (input != null) {
            String json = Serialization.asJson(input);
            try {
                return (O) Serialization.jsonMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(json, loadClass(className));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to deserialize json into an object of class:[" + className + "].");
            }
        }
        return null;
    }


    /**
     * Try to load a class using the current classloader (the one that loaded this class)
     * or the thread context class loader.
     * @param className The name of the class.
     * @return          The class of throws IllegalStateException.
     */
    protected Class loadClass(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (Exception e) {
            //ignore and fallback to TCCL
        }

        try {
            if (Thread.currentThread().getContextClassLoader() != null)
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (Exception e) {
            //ignore and fallback to TCCL
        }
        throw new IllegalStateException("Failed to load class: ["+className+"].");
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
        return session.get();
    }
}
