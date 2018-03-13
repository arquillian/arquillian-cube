package org.arquillian.cube.kubernetes;

import java.nio.file.Path;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

public class ResourceGeneratorBuilder {

    private Path pom;
    private String[] goals = new String[] {"package", "fabric8:build", "fabric8:resource"};
    private String namespace;

    public ResourceGeneratorBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ResourceGeneratorBuilder goals(String[] goals) {
        this.goals = goals;
        return this;
    }

    public ResourceGeneratorBuilder pluginConfigurationIn(Path pom) {
        this.pom = pom;
        return this;
    }

    public void build() {
        final BuiltProject builtProject = EmbeddedMaven
            .forProject(pom.toFile())
            .setQuiet()
            .useDefaultDistribution()
            .setDebugLoggerLevel()
            .setGoals(goals)
            .addProperty("fabric8.namespace", namespace)
            .ignoreFailure()
            .build();

        System.out.println(builtProject.getMavenLog());
        if (builtProject.getMavenBuildExitCode() != 0) {
            throw new IllegalStateException("Maven build has failed, see logs for details");
        }
    }
}
