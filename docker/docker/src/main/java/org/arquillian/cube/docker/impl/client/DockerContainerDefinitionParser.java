package org.arquillian.cube.docker.impl.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
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
    private static final String DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE = "docker-compose";

    private DockerContainerDefinitionParser() {
        super();
    }

    public static DockerCompositions convert(Path definitionFilePath, DefinitionFormat definitionFormat)
        throws IOException {
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
        } catch (FileSystemNotFoundException e) {
            String content = "";
            if (uri.isAbsolute()) {
                content = IOUtil.asStringPreservingNewLines(uri.toURL().openStream());
            } else {
                String fileContent = uri.toString();
                content = IOUtil.asStringPreservingNewLines(new FileInputStream(fileContent));
            }
            return convert(content, definitionFormat);
        } catch (IllegalArgumentException e) {
            String content = "";
            if (uri.isAbsolute()) {
                content = IOUtil.asStringPreservingNewLines(uri.toURL().openStream());
            } else {
                String fileContent = uri.toString();
                content = IOUtil.asStringPreservingNewLines(new FileInputStream(fileContent));
            }
            return convert(content, definitionFormat);
        }
    }

    public static DockerCompositions convert(String content, DefinitionFormat definitionFormat) {
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

    public static DockerCompositions convertDefault(DefinitionFormat definitionFormat) throws IOException {
        URI defaultUri = null;
        switch (definitionFormat) {
            case COMPOSE: {
                defaultUri = getDefaultFileLocation(DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE);
                break;
            }
            case CUBE: {
                defaultUri = getDefaultFileLocation(DEFAULT_CUBE_DEFINITION_FILE);
                break;
            }
            default: {
                defaultUri = getDefaultFileLocation(DEFAULT_DOCKER_COMPOSE_DEFINITION_FILE);
            }
        }
        if (defaultUri == null) {
            logger.fine(
                "No Docker container definitions has been found. Probably you have defined some Containers using Container Object pattern and @Cube annotation");
            return new DockerCompositions();
        }
        return convert(defaultUri, definitionFormat);
    }

    private static URI getDefaultFileLocation(String filename) {

        // src/{test/main}/docker
        URI docker = checkSrcTestAndMainResources(filename, "docker");

        if (docker == null) {

            // .
            docker = checkRoot(filename);

            if (docker == null) {
                // src/distribution
                docker = checkDistributionDirectory(filename);

                if (docker == null) {
                    // src/{test, main}/resources/docker
                    docker = checkSrcTestAndMainResources(filename, "resources/docker");

                    if (docker == null) {

                        // src/{test, main}/resources
                        docker = checkSrcTestAndMainResources(filename, "resources");
                    }
                }
            }
        }

        return docker;
    }

    private static URI checkRoot(String filename) {
        final Path rootPath = Paths.get(filename);
        final Path finalPath = resolveDockerDefinition(rootPath);

        if (finalPath != null) {
            return finalPath.toUri();
        }

        return null;
    }

    private static URI checkDistributionDirectory(String filename) {
        final Path rootPath = Paths.get("src", "distribution", filename);
        final Path finalPath = resolveDockerDefinition(rootPath);

        if (finalPath != null) {
            return finalPath.toUri();
        }

        return null;
    }

    /**
     * Checks if given file is at src/{test, main}/outerDirectory/filename exists or not.
     *
     * @param filename
     *     to search
     * @param outerDirectory
     *     to append after test or main
     *
     * @return Location of searched file
     */
    static URI checkSrcTestAndMainResources(String filename, String outerDirectory) {
        final Path testPath = Paths.get("src", "test", outerDirectory, filename);
        final Path testDefinitionPath = resolveDockerDefinition(testPath);
        if (testDefinitionPath != null) {
            return testDefinitionPath.toUri();
        } else {
            final Path mainPath = Paths.get("src", "main", outerDirectory, filename);
            final Path mainDefinitionPath = resolveDockerDefinition(mainPath);
            if (mainDefinitionPath != null) {
                return mainDefinitionPath.toUri();
            }
        }

        return null;
    }

    /**
     * Resolves current full path with .yml and .yaml extensions
     *
     * @param fullpath
     *     without extension.
     *
     * @return Path of existing definition or null
     */
    private static Path resolveDockerDefinition(Path fullpath) {
        final Path ymlPath = fullpath.resolveSibling(fullpath.getFileName() + ".yml");
        if (Files.exists(ymlPath)) {
            return ymlPath;
        } else {
            final Path yamlPath = fullpath.resolveSibling(fullpath.getFileName() + ".yaml");
            if (Files.exists(yamlPath)) {
                return yamlPath;
            }
        }

        return null;
    }
}
