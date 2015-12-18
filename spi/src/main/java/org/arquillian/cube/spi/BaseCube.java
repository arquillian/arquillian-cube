package org.arquillian.cube.spi;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseCube<X> implements Cube<X> {
    private Map<Class<?>, Object> metadata = new HashMap<>();

    @Override
    public boolean hasMetadata(Class<?> type) {
        return metadata.containsKey(type);
    }

    @Override
    public void addMetadata(Object type) {
        metadata.put(type.getClass(), type);
    }

    @Override
    public <T> T getMetadata(Class<T> type) {
        return (T) metadata.get(type);
    }
}
