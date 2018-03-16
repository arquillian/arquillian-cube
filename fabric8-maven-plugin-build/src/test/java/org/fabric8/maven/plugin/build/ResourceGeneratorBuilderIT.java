package org.fabric8.maven.plugin.build;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
public class ResourceGeneratorBuilderIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void should_generate_build_and_resources() throws IOException {
        // given
        final String rootPath = temporaryFolder.getRoot().toString() + "spring-boot-http-booster";
        copyDirectory(Paths.get("src/test/resources/spring-boot-http-booster"), Paths.get(rootPath));
        final String namespace = new DefaultKubernetesClient().getNamespace();

        // when
        new Fabric8MavenPluginResourceGeneratorBuilder()
            .namespace(namespace)
            .quiet()
            .pluginConfigurationIn(Paths.get(rootPath, "pom.xml"))
            .build();

        // then
        final File resources = Paths.get(rootPath, "target/classes/META-INF/fabric8").toFile();
        final File build = Paths.get(rootPath, "target/docker").toFile();

        assertThat(resources.listFiles()).isNotEmpty();
        assertThat(resources.listFiles()).contains(Paths.get(resources.toString(), "kubernetes.json").toFile(), Paths.get(
            resources.toString(), "openshift.json").toFile());
        assertThat(build.listFiles()).isNotEmpty();
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        final List<Path> sources = Files.walk(source).collect(toList());
        final List<Path> targets = sources.stream().map(source::relativize).map(target::resolve)
            .collect(toList());
        for (int i = 0; i < sources.size(); i++) {
            Files.copy(sources.get(i), targets.get(i));
        }
    }
}
