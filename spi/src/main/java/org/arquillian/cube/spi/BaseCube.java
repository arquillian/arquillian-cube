package org.arquillian.cube.spi;

import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.spi.metadata.CubeMetadata;

public abstract class BaseCube<T> implements Cube<T> {

    private Map<Class<? extends CubeMetadata>, Object> metadata = new HashMap<>();

    @Override
    public <X extends CubeMetadata> boolean hasMetadata(Class<X> type) {
        return metadata.containsKey(type);
    }

    @Override
    public <X extends CubeMetadata> void addMetadata(Class<X> type, X impl) {
        metadata.put(type, impl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X extends CubeMetadata> X getMetadata(Class<X> type) {
        return (X) metadata.get(type);
    }
}
