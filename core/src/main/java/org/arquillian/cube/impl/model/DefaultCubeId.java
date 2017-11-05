package org.arquillian.cube.impl.model;

public class DefaultCubeId implements CubeId {

    private final String id;

    public DefaultCubeId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isMatching(CubeId other) {
        return equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultCubeId that = (DefaultCubeId) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultCubeId{id='" + id + "'}";
    }
}
