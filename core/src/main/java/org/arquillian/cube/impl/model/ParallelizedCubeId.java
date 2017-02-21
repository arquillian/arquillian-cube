package org.arquillian.cube.impl.model;

public class ParallelizedCubeId implements CubeId {
    static final String PATTERN = "^(.*?)_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    private final String id;

    public ParallelizedCubeId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isMatching(CubeId other) {
        if (other instanceof ParallelizedCubeId) {
            return equals(other);
        }

        if (other instanceof StarredCubeId) {
            String otherId = other.getId();
            String otherStarlessId = otherId.substring(0, otherId.length() - 1);
            String uuidLessId = id.substring(0, id.lastIndexOf('_'));
            return uuidLessId.equals(otherStarlessId);
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

        ParallelizedCubeId that = (ParallelizedCubeId) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ParallelizedCubeId{id='" + id + "'}";
    }
}
