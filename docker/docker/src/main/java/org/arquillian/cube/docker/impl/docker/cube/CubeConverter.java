package org.arquillian.cube.docker.impl.docker.cube;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.arquillian.cube.docker.impl.client.Converter;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.util.ConfigUtil;

public class CubeConverter implements Converter {

    private final DockerCompositions dockerCubeDefinitionMap;

    private CubeConverter(Path location) throws IOException {
        FileInputStream inputStream = new FileInputStream(location.toFile());
        this.dockerCubeDefinitionMap = ConfigUtil.load(inputStream);
        inputStream.close();
    }

    private CubeConverter(String content) {
        this.dockerCubeDefinitionMap = ConfigUtil.load(content);
    }

    public static CubeConverter create(Path location) {
        try {
            return new CubeConverter(location);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static CubeConverter create(String content) {
        return new CubeConverter(content);
    }

    @Override
    public DockerCompositions convert() {
        return this.dockerCubeDefinitionMap;
    }
}
