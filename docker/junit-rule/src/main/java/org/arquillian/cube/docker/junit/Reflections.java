package org.arquillian.cube.docker.junit;


import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Reflections {

    public static final void injectObject(Object instance, Field field, Object injection) throws IllegalAccessException {

        field.setAccessible(true);
        field.set(instance, injection);

    }

    public static final List<Field> findAllFieldsOfType(Class<?> clazz, Class<?> fieldType, Predicate<Field> restriction) {

        final List<Field> fields = new ArrayList<>(findAllFieldsFilteringByType(clazz, fieldType, restriction));

        Class<?> currentClass = clazz;

        while (currentClass != Object.class) {
            currentClass = currentClass.getSuperclass();
            fields.addAll(findAllFieldsFilteringByType(currentClass, fieldType, restriction));
        }

        return fields;

    }

    private static final List<Field> findAllFieldsFilteringByType(Class<?> clazz, Class<?> fieldType, Predicate<Field> restriction) {
        return Arrays.stream(clazz.getDeclaredFields())
            .peek(f -> f.setAccessible(true))
            .filter(f -> f.getType().equals(fieldType))
            .filter(restriction)
            .collect(Collectors.toList());
    }

    public static final Optional<Field> findFieldByGenericType(Class<?> clazz, Class<?> fieldType, Class<?> genericType) {
        try {
            return Arrays.stream(clazz.getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .filter(f -> f.getType().equals(fieldType))
                .filter(f -> ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0].equals(genericType))
                .findAny();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}
