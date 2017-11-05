package org.arquillian.cube.impl.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * SecurityActions
 * <p>
 * A set of privileged actions that are not to leak out of this package
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public final class ReflectionUtil {

    // -------------------------------------------------------------------------------||
    // Constructor
    // ------------------------------------------------------------------||
    // -------------------------------------------------------------------------------||

    /**
     * No instantiation
     */
    private ReflectionUtil() {
        throw new UnsupportedOperationException("No instantiation");
    }

    // -------------------------------------------------------------------------------||
    // Utility Methods
    // --------------------------------------------------------------||
    // -------------------------------------------------------------------------------||

    /**
     * Obtains the Thread Context ClassLoader
     */
    public static ClassLoader getThreadContextClassLoader() {
        return AccessController.doPrivileged(GetTcclAction.INSTANCE);
    }

    /**
     * Obtains the Constructor specified from the given Class and argument types
     *
     * @throws NoSuchMethodException
     */
    public static Constructor<?> getConstructor(final Class<?> clazz,
        final Class<?>... argumentTypes) throws NoSuchMethodException {
        try {
            return AccessController
                .doPrivileged(new PrivilegedExceptionAction<Constructor<?>>() {
                    public Constructor<?> run()
                        throws NoSuchMethodException {
                        return clazz.getConstructor(argumentTypes);
                    }
                });
        }
        // Unwrap
        catch (final PrivilegedActionException pae) {
            final Throwable t = pae.getCause();
            // Rethrow
            if (t instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) t;
            } else {
                // No other checked Exception thrown by Class.getConstructor
                try {
                    throw (RuntimeException) t;
                }
                // Just in case we've really messed up
                catch (final ClassCastException cce) {
                    throw new RuntimeException(
                        "Obtained unchecked Exception; this code should never be reached",
                        t);
                }
            }
        }
    }

    /**
     * Create a new instance by finding a constructor that matches the
     * argumentTypes signature using the arguments for instantiation.
     *
     * @param className
     *     Full classname of class to create
     * @param argumentTypes
     *     The constructor argument types
     * @param arguments
     *     The constructor arguments
     *
     * @return a new instance
     *
     * @throws IllegalArgumentException
     *     if className, argumentTypes, or arguments are null
     * @throws RuntimeException
     *     if any exceptions during creation
     * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
     * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
     */
    public static <T> T newInstance(final String className,
        final Class<?>[] argumentTypes, final Object[] arguments,
        final Class<T> expectedType) {
        if (className == null) {
            throw new IllegalArgumentException("ClassName must be specified");
        }
        if (argumentTypes == null) {
            throw new IllegalArgumentException(
                "ArgumentTypes must be specified. Use empty array if no arguments");
        }
        if (arguments == null) {
            throw new IllegalArgumentException(
                "Arguments must be specified. Use empty array if no arguments");
        }
        final Object obj;
        try {
            final ClassLoader tccl = getThreadContextClassLoader();
            final Class<?> implClass = Class.forName(className, false, tccl);
            Constructor<?> constructor = getConstructor(implClass,
                argumentTypes);
            obj = constructor.newInstance(arguments);
        } catch (Exception e) {
            throw new RuntimeException("Could not create new instance of "
                + className + ", missing package from classpath?", e);
        }

        // Cast
        try {
            return expectedType.cast(obj);
        } catch (final ClassCastException cce) {
            // Reconstruct so we get some useful information
            throw new ClassCastException("Incorrect expected type, "
                + expectedType.getName() + ", defined for "
                + obj.getClass().getName());
        }
    }

    public static boolean isClassPresent(String name) {
        try {
            ClassLoader classLoader = getThreadContextClassLoader();
            classLoader.loadClass(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static List<Field> getFieldsWithAnnotation(final Class<?> source,
        final Class<? extends Annotation> annotationClass) {
        List<Field> declaredAccessableFields = AccessController
            .doPrivileged(new PrivilegedAction<List<Field>>() {
                public List<Field> run() {
                    List<Field> foundFields = new ArrayList<Field>();
                    Class<?> nextSource = source;
                    while (nextSource != Object.class) {
                        for (Field field : nextSource.getDeclaredFields()) {
                            if (field.isAnnotationPresent(annotationClass)) {
                                if (!field.isAccessible()) {
                                    field.setAccessible(true);
                                }
                                foundFields.add(field);
                            }
                        }
                        nextSource = nextSource.getSuperclass();
                    }
                    return foundFields;
                }
            });
        return declaredAccessableFields;
    }

    public static boolean isClassWithAnnotation(final Class<?> source,
        final Class<? extends Annotation> annotationClass) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                boolean annotationPresent = false;
                Class<?> nextSource = source;
                while (nextSource != Object.class) {
                    if (nextSource.isAnnotationPresent(annotationClass)) {
                        return true;
                    }
                    nextSource = nextSource.getSuperclass();
                }
                return annotationPresent;
            }
        });
    }

    public static List<Method> getMethodsWithAnnotation(final Class<?> source,
        final Class<? extends Annotation> annotationClass) {
        List<Method> declaredAccessableMethods = AccessController
            .doPrivileged(new PrivilegedAction<List<Method>>() {
                public List<Method> run() {
                    List<Method> foundMethods = new ArrayList<Method>();
                    for (Method method : source.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(annotationClass)) {
                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            foundMethods.add(method);
                        }
                    }
                    return foundMethods;
                }
            });
        return declaredAccessableMethods;
    }

    // -------------------------------------------------------------------------------||
    // Inner Classes
    // ----------------------------------------------------------------||
    // -------------------------------------------------------------------------------||

    /**
     * Single instance to get the TCCL
     */
    private enum GetTcclAction implements PrivilegedAction<ClassLoader> {
        INSTANCE;

        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }

    }
}
