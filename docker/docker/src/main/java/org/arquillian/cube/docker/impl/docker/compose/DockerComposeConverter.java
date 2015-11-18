package org.arquillian.cube.docker.impl.docker.compose;

import org.arquillian.cube.docker.impl.client.Converter;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.IOUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asMap;

public class DockerComposeConverter implements Converter {

    private Map<String, Object> dockerComposeDefinitionMap = new HashMap<>();
    private Path dockerComposeRootDirectory;

    private DockerComposeConverter(Path location) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(location.toFile())) {
            String content = IOUtil.asStringPreservingNewLines(inputStream);
            content = resolvePlaceholders(content);
            this.dockerComposeDefinitionMap = (Map<String, Object>) new Yaml().load(content);
            this.dockerComposeRootDirectory = location.getParent();
        }
    }

    private String resolvePlaceholders(String content) {
        final Map<String, String> env = System.getenv();
        return IOUtil.replacePlaceholdersWithWhiteSpace(content, env);
    }

    private DockerComposeConverter(String content) {
        String resolvePlaceholders = resolvePlaceholders(content);
        this.dockerComposeDefinitionMap = (Map<String, Object>) new Yaml().load(content);
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

    public Map<String, Object> convert() {
        Map<String, Object> dockerCubeDefinitionMap = new HashMap<>();

        Set<String> names = dockerComposeDefinitionMap.keySet();

        for(String name : names) {
            Map<String, Object> containerCubeDefinition = convertContainer(asMap(dockerComposeDefinitionMap, name));
            dockerCubeDefinitionMap.put(name, containerCubeDefinition);
        }
        return dockerCubeDefinitionMap;
    }

    private Map<String, Object> convertContainer(Map<String, Object> dockerComposeContainerDefinition) {
        ContainerBuilder containerBuilder = new ContainerBuilder(this.dockerComposeRootDirectory);
        Map<String, Object> conf = containerBuilder.build(dockerComposeContainerDefinition);
        if (conf.containsKey(DockerClientExecutor.ENV)) {
            conf.put(DockerClientExecutor.ENV, toEnvironment((Properties) conf.get(DockerClientExecutor.ENV)));
        }
        return conf;
    }
    private Collection<String> toEnvironment(Properties properties) {
        Set<String> listOfEnvironment = new HashSet<>();
        Set<Entry<Object, Object>> entrySet = properties.entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            listOfEnvironment.add(entry.getKey() + "=" + entry.getValue());
        }
        return listOfEnvironment;
    }

}
