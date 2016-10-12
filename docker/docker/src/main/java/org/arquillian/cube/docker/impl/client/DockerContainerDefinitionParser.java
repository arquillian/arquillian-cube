package org.arquillian.cube.docker.impl.client;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.docker.compose.DockerComposeConverter;
import org.arquillian.cube.docker.impl.docker.cube.CubeConverter;
import org.arquillian.cube.impl.util.IOUtil;

public class DockerContainerDefinitionParser {

    private static final Logger logger = Logger.getLogger(DockerContainerDefinitionParser.class.getName());

    private static final String DEFAULT_CUBE_DEFINITION_FILE = "cube";
    private static final String DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE = "docker-compose.yml";

    private DockerContainerDefinitionParser() {
        super();
    }

    public static DockerCompositions convert(Path definitionFilePath, DefinitionFormat definitionFormat) throws IOException {
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

    public static DockerCompositions convert(DefinitionFormat definitionFormat, URI... uris) throws IOException {
        DockerCompositions finalDefinition = new DockerCompositions();
        for (URI uri : uris) {
            DockerCompositions convertedDocument = convert(uri, definitionFormat);
            finalDefinition.merge(convertedDocument);
        }

        return finalDefinition;
    }

    private static DockerCompositions convert(URI uri, DefinitionFormat definitionFormat) throws IOException {
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
            String fileLocation = "";
            if(uri.isAbsolute()) {
                content = IOUtil.asStringPreservingNewLines(uri.toURL().openStream());
            } else {
                fileLocation = uri.toString();
                content = IOUtil.asStringPreservingNewLines(new FileInputStream(fileLocation));
            }
            return convert(content, definitionFormat, fileLocation);
        }
    }

    public  static DockerCompositions convert(String content, DefinitionFormat definitionFormat, String composeLocation) {
        switch (definitionFormat) {
            case COMPOSE: {
                DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(content, composeLocation);
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

    public static DockerCompositions convert(String content, DefinitionFormat definitionFormat) {
        return convert(content, definitionFormat, null);
    }

    public static DockerCompositions convertDefault(DefinitionFormat definitionFormat) throws IOException {
        URI defaultUri = null;
        try {
            switch (definitionFormat) {
                case COMPOSE: {
                    final URL resource = DockerContainerDefinitionParser.class.getResource("/" + DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE);
                    if (resource != null) {
                        defaultUri = resource.toURI();
                    }
                    break;
                }
                case CUBE: {
                    final URL resource = DockerContainerDefinitionParser.class.getResource("/" + DEFAULT_CUBE_DEFINITION_FILE);
                    if (resource != null) {
                        defaultUri = resource.toURI();
                    }
                    break;
                }
                default: {
                    final URL resource = DockerContainerDefinitionParser.class.getResource("/" + DEFAULT_CUBE_DEFINITION_FILE);
                    if (resource != null) {
                        defaultUri = resource.toURI();
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (defaultUri == null) {
            logger.fine("No Docker container definitions has been found. Probably you have defined some Containers using Container Object pattern and @Cube annotation");
            return new DockerCompositions();
        }
        return convert(Paths.get(defaultUri), definitionFormat);
    }
}
