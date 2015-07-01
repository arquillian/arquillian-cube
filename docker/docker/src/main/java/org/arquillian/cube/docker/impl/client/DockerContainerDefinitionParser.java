package org.arquillian.cube.docker.impl.client;


import org.arquillian.cube.docker.impl.docker.cube.CubeConverter;
import org.arquillian.cube.docker.impl.docker.compose.DockerComposeConverter;
import org.arquillian.cube.docker.impl.util.IOUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DockerContainerDefinitionParser {

    private static final String DEFAULT_CUBE_DEFINITION_FILE = "cube";
    private static final String DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE = "docker-compose.yml";

    private DockerContainerDefinitionParser() {
        super();
    }

    public static Map<String, Object> convert(Path definitionFilePath, DefinitionFormat definitionFormat) throws IOException {
        switch (definitionFormat) {
            case COMPOSE: {
                DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(definitionFilePath);
                return dockerComposeConverter.convert();
            }
            case CUBE: {
                CubeConverter cubeConverter = CubeConverter.create(definitionFilePath);
                return cubeConverter.convert();
            }
            default: {
                CubeConverter cubeConverter = CubeConverter.create(definitionFilePath);
                return cubeConverter.convert();
            }
        }
    }

    public static Map<String, Object> convert(URI uri, DefinitionFormat definitionFormat) throws IOException {
        try {
            Path definitionFilePath = Paths.get(uri);
            return convert(definitionFilePath, definitionFormat);
        } catch(FileSystemNotFoundException e) {
            String content = "";
            if(uri.isAbsolute()) {
                content = IOUtil.asStringPreservingNewLines(uri.toURL().openStream());
            } else {
                String fileContent = uri.toString();
                content = IOUtil.asStringPreservingNewLines(new FileInputStream(fileContent));
            }
            return convert(content, definitionFormat);
        } catch(IllegalArgumentException e) {
            String content = "";
            if(uri.isAbsolute()) {
                content = IOUtil.asStringPreservingNewLines(uri.toURL().openStream());
            } else {
                String fileContent = uri.toString();
                content = IOUtil.asStringPreservingNewLines(new FileInputStream(fileContent));
            }
            return convert(content, definitionFormat);
        }
    }

    public static Map<String, Object> convert(String content, DefinitionFormat definitionFormat) {
        switch (definitionFormat) {
            case COMPOSE: {
                DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(content);
                return dockerComposeConverter.convert();
            }
            case CUBE: {
                CubeConverter cubeConverter = CubeConverter.create(content);
                return cubeConverter.convert();
            }
            default: {
                CubeConverter cubeConverter = CubeConverter.create(content);
                return cubeConverter.convert();
            }
        }
    }

    public static Map<String, Object> convertDefault(DefinitionFormat definitionFormat) throws IOException {
        URI defaultUri = null;
        try {
            switch (definitionFormat) {
                case COMPOSE: {
                    defaultUri = DockerContainerDefinitionParser.class.getResource("/" + DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE).toURI();
                    break;
                }
                case CUBE: {
                    defaultUri = DockerContainerDefinitionParser.class.getResource("/" + DEFAULT_CUBE_DEFINITION_FILE).toURI();
                    break;
                }
                default: {
                    defaultUri = DockerContainerDefinitionParser.class.getResource("/" + DEFAULT_CUBE_DEFINITION_FILE).toURI();
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (defaultUri == null) {
            throw new IllegalArgumentException(String.format("No location was specified and no default definition was found in root of classpath for %s. CUBE [%s] COMPOSE [%s].",
                    definitionFormat, DEFAULT_CUBE_DEFINITION_FILE, DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE));
        }
        return convert(Paths.get(defaultUri), definitionFormat);
    }
}
