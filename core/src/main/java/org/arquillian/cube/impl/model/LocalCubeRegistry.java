package org.arquillian.cube.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.CubeMetadata;


public class LocalCubeRegistry implements CubeRegistry {

    private Map<CubeId, Cube<?>> cubes;

    public LocalCubeRegistry() {
        this.cubes = new HashMap<>();
    }

    @Override
    public void addCube(Cube<?> cube) {
        CubeId cubeId = CubeIdFactory.get().create(cube.getId());

        if(cubeId instanceof StarredCubeId){
            throw new IllegalArgumentException("Starred cube id cannot be added.");
        }

        this.cubes.put(cubeId, cube);
    }

    @Override
    public void removeCube(String id) {
        CubeId cubeId = CubeIdFactory.get().create(id);

        if(cubeId instanceof StarredCubeId){
            throw new IllegalArgumentException("Starred cube id cannot be removed.");
        }

        cubes.remove(cubeId);
    }

    @Override
    public List<Cube<?>> getByMetadata(Class<? extends CubeMetadata> metadata) {
        List<Cube<?>> cubes = new ArrayList<>();
        for (Cube<?> cube : this.cubes.values()) {
            if (cube.hasMetadata(metadata)) {
                cubes.add(cube);
            }
        }
        return cubes;
    }

    @Override
    public Cube<?> getCube(String id) {
        CubeId cubeId = CubeIdFactory.get().create(id);
        for(Map.Entry<CubeId,Cube<?>> cubeEntry : this.cubes.entrySet()) {
            CubeId internalCubeId = cubeEntry.getKey();
            if(internalCubeId.isMatching(cubeId)) {
                return cubeEntry.getValue();
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
        List<Cube<?>> cubeList = new ArrayList<>(this.cubes.values());
        return Collections.unmodifiableList(cubeList);
    }

}
