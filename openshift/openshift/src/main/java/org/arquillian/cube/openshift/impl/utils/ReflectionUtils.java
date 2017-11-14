/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.arquillian.cube.openshift.impl.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ReflectionUtils {
    public static Method findDeploymentMethod(Class<?> clazz) {
        List<Method> methods = findDeploymentMethods(clazz);
        return (methods.size() == 0) ? null : methods.iterator().next();
    }

    public static List<Method> findDeploymentMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        findAnnotatedMethods(clazz, Deployment.class, methods);
        return methods;
    }

    public static Method findAnnotatedMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        List<Method> methods = new ArrayList<>();
        findAnnotatedMethods(clazz, annotationClass, methods);
        return (methods.size() == 0) ? null : methods.iterator().next();
    }

    public static void findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationClass, List<Method> list) {
        if (clazz == Object.class) {
            return;
        }

        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers()) && m.isAnnotationPresent(annotationClass) && getParameterCount(m) == 0 && !m.isBridge()) {
                list.add(m);
            }
        }

        findAnnotatedMethods(clazz.getSuperclass(), annotationClass, list);
    }

    public static <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> annotationClass) {
        if (clazz == Object.class) {
            return null;
        }

        T annotation = clazz.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        } else {
            return findAnnotation(clazz.getSuperclass(), annotationClass);
        }
    }

    public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return clazz != Object.class && (clazz.isAnnotationPresent(annotationClass) || isAnnotationPresent(clazz.getSuperclass(), annotationClass));

    }

    public static <T> T invoke(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object instance, Object[] args, Class<T> returnType) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return returnType.cast(method.invoke(instance, args));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int getParameterCount(Method method) {
        return method.getParameterTypes().length; // use method.getParameterCount() on Java8
    }
}
