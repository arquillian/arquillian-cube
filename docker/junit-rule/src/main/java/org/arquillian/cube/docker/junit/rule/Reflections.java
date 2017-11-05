package org.arquillian.cube.docker.junit.rule;


import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Optional;

class Reflections {

    public static final void injectObject(Object instance, Field field, Object injection) throws NoSuchFieldException, IllegalAccessException {

        field.setAccessible(true);
        field.set(instance, injection);

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
