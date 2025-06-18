package org.fabric8.maven.plugin.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.remote.requirement.RequiresRemoteResource;
import org.eclipse.jkube.maven.plugin.build.JKubeMavenPluginResourceGeneratorBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

// If you want to run it from the IDE against latest code changes you have to set system property `arquillian-cube.version` with latest project.version.

@Category({RequiresOpenshift.class, RequiresRemoteResource.class})
@RequiresOpenshift
public class OpenShiftResourceGeneratorBuilderIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String namespace;
    private KubernetesClient kubernetesClient;

    @Before
    public void getNamespace() {
        kubernetesClient = new DefaultKubernetesClient();
        namespace = kubernetesClient.getNamespace();
    }

    @Test
    public void should_build_images_and_generate_resources_with_embedded_maven() throws IOException {
        should_build_images_and_generate_resources(false);
    }

    @Test
    public void should_build_images_and_generate_resources_with_local_maven() throws IOException {
        should_build_images_and_generate_resources(true);
    }

    private void should_build_images_and_generate_resources(boolean useLocalMaven) throws IOException {
        // given
        final String rootPath = temporaryFolder.getRoot().toString() + "spring-boot";
        copyDirectory(Paths.get("src/test/resources/spring-boot"), Paths.get(rootPath));

        // when
        new JKubeMavenPluginResourceGeneratorBuilder()
            .namespace(namespace)
            .quiet()
            .profiles("openshift")
            .quiet(false)
            .withProperties("version.cube", System.getProperty("version.cube", "2.0.0-SNAPSHOT"))
            .pluginConfigurationIn(Paths.get(rootPath, "pom.xml"))
            .forOpenshift(true)
            .withMaven(useLocalMaven)
            .build();

        // then
        final File resources = Paths.get(rootPath, "target/classes/META-INF/jkube/openshift").toFile();

        assertThat(resources.listFiles()).isNotEmpty();
        assertThat(resources.listFiles()).contains(Paths.get(resources.toString(), "spring-boot-service.yml").toFile(),
            Paths.get(resources.toString(), "spring-boot-route.yml").toFile(),
            Paths.get(resources.toString(), "spring-boot-deploymentconfig.yml").toFile());
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        final List<Path> sources = Files.walk(source).collect(toList());
        final List<Path> targets = sources.stream().map(source::relativize).map(target::resolve)
            .collect(toList());
        for (int i = 0; i < sources.size(); i++) {
            Files.copy(sources.get(i), targets.get(i));
        }
    }

    @After
    public void removeBuildPod() {
        final List<String> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems().stream()
            .filter(pod -> pod.getMetadata().getName().startsWith("spring-boot-s2i"))
            .map(pod -> pod.getMetadata().getName())
            .collect(Collectors.toList());

        assertThat(pods).isNotEmpty();
        assertThat(pods).size().isEqualTo(1);

        for (String pod : pods) {
            kubernetesClient.pods().inNamespace(namespace).withName(pod).delete();
        }
    }
}
