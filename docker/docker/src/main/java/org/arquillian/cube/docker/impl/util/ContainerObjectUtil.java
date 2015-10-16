package org.arquillian.cube.docker.impl.util;

import org.arquillian.cube.containerobject.Cube;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

public class ContainerObjectUtil {

    private ContainerObjectUtil() {
        super();
    }

    public static <T> T getTopCubeAttribute(final Class<?> source, final String nameField, final Class<? extends Annotation> annotationClass, final T defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                Method field = null;
                try {
                    field = annotationClass.getMethod(nameField);
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                }
                Class<?> nextSource = source;
                boolean foundAnnotation = false;
                while (nextSource != Object.class) {
                    if (nextSource.isAnnotationPresent(annotationClass)) {
                        foundAnnotation = true;
                        final Annotation annotation = nextSource.getAnnotation(annotationClass);
                        final T value = getValue(annotation, field);
                        //If it is not the deafult value means that we have found a top level definition
                        if (defaultValue.getClass().isArray()) {
                            //Not works with native elements but for now it is ok
                            if (!Arrays.equals((Object[])value, (Object[])defaultValue)) {
                                return (T) value;
                            }
                        } else {
                            if (!value.equals(defaultValue)) {
                                return (T) value;
                            }
                        }
                    }
                    //If not maybe we need to use the default value but maybe there is some parent class
                    //That contains a different value rather than the default ones so we need to continue search.
                    nextSource = source.getSuperclass();
                }
                return foundAnnotation ? defaultValue : null;
            }
        });
    }

    private static <T> T getValue(final Annotation annotation, Method field) {
        try {
            return (T)field.invoke(annotation);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
