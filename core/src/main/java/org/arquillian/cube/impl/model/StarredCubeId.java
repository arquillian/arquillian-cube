package org.arquillian.cube.impl.model;

public class StarredCubeId implements CubeId {
    static final String PATTERN = "^(.*?)\\*$";

    private final String id;

    public StarredCubeId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isMatching(CubeId other) {
        if (other instanceof StarredCubeId) {
            return equals(other);
        }

        if (other instanceof ParallelizedCubeId) {
            String otherId = other.getId();
            String starlessId = id.substring(0, id.length() - 1);
            return otherId.startsWith(starlessId);
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StarredCubeId that = (StarredCubeId) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StarredCubeId{id='" + id + "'}";
    }
}
