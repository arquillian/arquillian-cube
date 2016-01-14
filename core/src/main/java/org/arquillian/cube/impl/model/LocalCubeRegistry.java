package org.arquillian.cube.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;

public class LocalCubeRegistry implements CubeRegistry {

    private List<Cube<?>> cubes;

    public LocalCubeRegistry() {
        this.cubes = new ArrayList<Cube<?>>();
    }

    @Override
    public void addCube(Cube<?> cube) {
        this.cubes.add(cube);
    }

    @Override
    public void removeCube(String id) {
        for (int i = 0 ; i < this.cubes.size(); i++) {
            if (this.cubes.get(i).getId().equals(id)) {
                this.cubes.remove(i);
                break;
            }
        }
    }

    @Override
    public List<Cube<?>> getByMetadata(Class<?> metadata) {
        List<Cube<?>> cubes = new ArrayList<>();
        for (Cube<?> cube : this.cubes) {
            if (cube.hasMetadata(metadata)) {
                cubes.add(cube);
            }
        }
        return cubes;
    }

    @Override
    public Cube<?> getCube(String id) {
        for(Cube<?> cube : this.cubes) {
            if(cube.getId().equals(id)) {
                return cube;
            }
        }
        return null;
    }

    @Override
    public <T extends Cube<?>> T getCube(String id, Class<T> type) {
        return type.cast(getCube(id));
    }

    @Override
    public List<Cube<?>> getCubes() {
        return Collections.unmodifiableList(cubes);
    }
}
