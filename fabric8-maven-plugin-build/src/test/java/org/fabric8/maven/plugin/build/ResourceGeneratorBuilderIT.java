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
import org.eclipse.jkube.maven.plugin.build.OpenshiftMavenPluginResourceGeneratorBuilder;
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
public class ResourceGeneratorBuilderIT {

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
    public void should_build_images_and_generate_resources() throws IOException {
        //TODO:Move to jkube, we have to upgrade this parent pom
        //https://repo1.maven.org/maven2/io/openshift/booster/spring-boot-booster-parent/1.5.10-1/spring-boot-booster-parent-1.5.10-1.pom
        //There is openshift profile to enable fabric8-maven-plugin to resource and build.
        // given
        final String rootPath = temporaryFolder.getRoot().toString() + "spring-boot-http-booster";
        copyDirectory(Paths.get("src/test/resources/spring-boot-http-booster"), Paths.get(rootPath));

        // when
        new OpenshiftMavenPluginResourceGeneratorBuilder()
            .namespace(namespace)
            .quiet()
            .withProperties("version.cube", System.getProperty("version.cube", "1.15.3"))
            .pluginConfigurationIn(Paths.get(rootPath, "pom.xml"))
            .build();

        // then
        final File resources = Paths.get(rootPath, "target/classes/META-INF/jkube/openshift").toFile();
        //final File build = Paths.get(rootPath, "target/docker").toFile();

        assertThat(resources.listFiles()).isNotEmpty();
        assertThat(resources.listFiles()).contains(Paths.get(resources.toString(), "spring-boot-rest-http-service.yml").toFile(), Paths.get(
            resources.toString(), "spring-boot-rest-http-route.yml").toFile(), Paths.get(
                        resources.toString(), "spring-boot-rest-http-deploymentconfig.yml").toFile());
        //assertThat(build.listFiles()).isNotEmpty();
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
            .filter(pod -> pod.getMetadata().getName().startsWith("spring-boot-rest-http-s2i"))
            .map(pod -> pod.getMetadata().getName())
            .collect(Collectors.toList());

        for (String pod : pods) {
            kubernetesClient.pods().inNamespace(namespace).withName(pod).delete();
        }
    }
}
