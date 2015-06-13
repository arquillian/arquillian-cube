package org.arquillian.cube.docker.impl.docker.cube;

import org.arquillian.cube.docker.impl.client.Converter;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class CubeConverter implements Converter {

    private final Map<String, Object> dockerCubeDefinitionMap;

    private CubeConverter(Path location) throws IOException {
        FileInputStream inputStream = new FileInputStream(location.toFile());
        this.dockerCubeDefinitionMap = ConfigUtil.applyExtendsRules((Map<String, Object>) new Yaml().load(inputStream));
        inputStream.close();
    }

    private CubeConverter(String content) {
        this.dockerCubeDefinitionMap = ConfigUtil.applyExtendsRules((Map<String, Object>) new Yaml().load(content));
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
    public Map<String, Object> convert() {
        return this.dockerCubeDefinitionMap;
    }
}
