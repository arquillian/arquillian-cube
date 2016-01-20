package org.arquillian.cube.docker.impl.docker.compose;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.arquillian.cube.docker.impl.client.Converter;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.arquillian.cube.impl.util.IOUtil;
import org.yaml.snakeyaml.Yaml;

public class DockerComposeConverter implements Converter {

    private Map<String, Object> dockerComposeDefinitionMap = new HashMap<>();
    private Path dockerComposeRootDirectory;

    private DockerComposeConverter(Path location) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(location.toFile())) {
            String content = IOUtil.asStringPreservingNewLines(inputStream);
            content = resolvePlaceholders(content);
            this.dockerComposeDefinitionMap = loadConfig(content);
            this.dockerComposeRootDirectory = location.getParent();
        }
    }

    private String resolvePlaceholders(String content) {
        content = resolveSystemProperties(content);
        final Map<String, String> env = System.getenv();
        return IOUtil.replacePlaceholdersWithWhiteSpace(content, env);
    }

    private String resolveSystemProperties(String content) {
        return IOUtil.replacePlaceholdersWithWhiteSpace(content);
    }

    private DockerComposeConverter(String content) {
        String resolvePlaceholders = resolvePlaceholders(content);
        this.dockerComposeDefinitionMap = loadConfig(resolvePlaceholders);
        this.dockerComposeRootDirectory = Paths.get(".");
    }

    public static DockerComposeConverter create(Path location) {
        try {
            return new DockerComposeConverter(location);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static DockerComposeConverter create(String content) {
        return new DockerComposeConverter(content);
    }

    public CubeContainers convert() {
        CubeContainers cubeContainers = new CubeContainers();

        Set<String> names = dockerComposeDefinitionMap.keySet();

        for(String name : names) {
            CubeContainer cubeContainer = convertContainer(asMap(dockerComposeDefinitionMap, name));
            cubeContainers.add(name, cubeContainer);
        }
        return cubeContainers;
    }

    private CubeContainer convertContainer(Map<String, Object> dockerComposeContainerDefinition) {
        ContainerBuilder containerBuilder = new ContainerBuilder(this.dockerComposeRootDirectory);
        return containerBuilder.build(dockerComposeContainerDefinition);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig(String content) {
        return (Map<String, Object>) new Yaml().load(content);
    }
}
