package org.arquillian.cube.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;

public class DockerCubeRegistry implements CubeRegistry {

    private List<Cube> cubes;

    public DockerCubeRegistry() {
        this.cubes = new ArrayList<Cube>();
    }

    @Override
    public void addCube(Cube cube) {
        this.cubes.add(cube);
    }

    @Override
    public Cube getCube(String id) {
        for(Cube cube : this.cubes) {
            if(cube.getId().equals(id)) {
                return cube;
            }
        }
        return null;
    }

    @Override
    public List<Cube> getCubes() {
        return Collections.unmodifiableList(cubes);
    }
}
