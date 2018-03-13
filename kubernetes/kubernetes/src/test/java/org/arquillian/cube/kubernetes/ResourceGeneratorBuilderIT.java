package org.arquillian.cube.kubernetes;

import java.io.File;
import java.nio.file.Paths;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.smart.testing.rules.git.GitClone;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
public class ResourceGeneratorBuilderIT {

    @ClassRule
    public static final GitClone gitClone = new GitClone("https://github.com/snowdrop/spring-boot-http-booster");

    @Test
    public void should_generate_build_and_resources() {
        // given
        final String folder = gitClone.getGitRepoFolder().getAbsolutePath();

        // when
        new ResourceGeneratorBuilder()
            .namespace("myproject")
            .pluginConfigurationIn(Paths.get(folder, "pom.xml"))
            .build();

        // then
        final File resources = Paths.get(folder, "target/classes/META-INF/fabric8").toFile();
        final File build = Paths.get(folder, "target/docker").toFile();

        assertThat(resources.listFiles()).isNotEmpty();
        assertThat(resources.listFiles()).contains(Paths.get(resources.toString(), "kubernetes.json").toFile(), Paths.get(
            resources.toString(), "openshift.json").toFile());
        assertThat(build.listFiles()).isNotEmpty();
    }
}
