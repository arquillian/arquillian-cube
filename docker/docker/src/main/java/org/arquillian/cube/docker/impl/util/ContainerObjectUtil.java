package org.arquillian.cube.docker.impl.util;

import org.arquillian.cube.containerobject.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContainerObjectUtil {

   private ContainerObjectUtil() {
      super();
   }

   public static List<? extends Annotation> getAllAnnotations(final Class<?> source, final Class<? extends Annotation> annotationClass) {
      return AccessController.doPrivileged((PrivilegedAction<List<? extends Annotation>>) () -> {
         List<Annotation> annotations = new ArrayList<>();

         Class<?> nextSource = source;
         while (nextSource != Object.class) {
            final Annotation[] annotationsByType = nextSource.getAnnotationsByType(annotationClass);
            Collections.addAll(annotations, annotationsByType);
            //If not maybe we need to use the default value but maybe there is some parent class
            //That contains a different value rather than the default ones so we need to continue search.
            nextSource = nextSource.getSuperclass();
         }
         return annotations;
      });
   }

   public static <T> T getTopCubeAttribute(final Class<?> source, final String nameField, final Class<? extends Annotation> annotationClass, final T defaultValue) {
      return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
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
               //If it is not the default value means that we have found a top level definition
               if (defaultValue.getClass().isArray()) {

                  if (defaultValue.getClass().getComponentType().isPrimitive()) {
                     // Only works with integer primitives or CCE
                     if (!Arrays.equals((int[]) value, (int[]) defaultValue)) {
                        return value;
                     }
                  } else {

                     if (!Arrays.equals((Object[]) value, (Object[]) defaultValue)) {
                        return value;
                     }
                  }
               } else {
                  if (!value.equals(defaultValue)) {
                     return value;
                  }
               }
            }
            //If not maybe we need to use the default value but maybe there is some parent class
            //That contains a different value rather than the default ones so we need to continue search.
            nextSource = nextSource.getSuperclass();
         }
         return foundAnnotation ? defaultValue : null;
      });
   }

   @SuppressWarnings("unchecked")
   private static <T> T getValue(final Annotation annotation, Method field) {
      try {
         return (T) field.invoke(annotation);
      } catch (InvocationTargetException | IllegalAccessException e) {
         throw new IllegalArgumentException(e);
      }
   }
}
