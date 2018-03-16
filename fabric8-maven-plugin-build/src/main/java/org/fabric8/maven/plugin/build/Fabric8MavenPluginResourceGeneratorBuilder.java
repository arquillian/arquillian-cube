package org.fabric8.maven.plugin.build;

import java.nio.file.Path;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.pom.equipped.ConfigurationDistributionStage;

public class Fabric8MavenPluginResourceGeneratorBuilder {

    private Path pom;
    private String[] goals = new String[] {"package", "fabric8:build", "fabric8:resource"};
    private String namespace;
    private boolean mvnDebugOutput;
    private boolean quietMode;

    public Fabric8MavenPluginResourceGeneratorBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Fabric8MavenPluginResourceGeneratorBuilder goals(String[] goals) {
        this.goals = goals;
        return this;
    }

    public Fabric8MavenPluginResourceGeneratorBuilder pluginConfigurationIn(Path pom) {
        this.pom = pom;
        return this;
    }

    public Fabric8MavenPluginResourceGeneratorBuilder quiet(boolean quiet) {
        this.quietMode = quiet;
        return this;
    }

    /**
     * Enables mvn debug output (-X) flag. Implies build logging output.
     */
    private Fabric8MavenPluginResourceGeneratorBuilder withDebugOutput(boolean debug) {
        this.mvnDebugOutput = debug;
        quiet(!debug);
        return this;
    }

    public Fabric8MavenPluginResourceGeneratorBuilder debug(boolean debug) {
        this.mvnDebugOutput = debug;
        return this;
    }

    public void build() {
        final ConfigurationDistributionStage distributionStage = EmbeddedMaven
            .forProject(pom.toFile())
            .setQuiet(quietMode)
            .useDefaultDistribution()
            .setDebug(mvnDebugOutput)
            .setDebugLoggerLevel()
            .setGoals(goals)
            .addProperty("fabric8.namespace", namespace);

        //Remove this after fixing issue  https://github.com/eclipse/che/issues/9105
        if (System.getenv("JAVA_HOME") == null) {
            distributionStage.addShellEnvironment("JAVA_HOME", "/usr/lib/jvm/java-1.8.0");
        }

        final BuiltProject builtProject = distributionStage.ignoreFailure()
            .build();

        if (builtProject.getMavenBuildExitCode() != 0) {
            System.out.println(builtProject.getMavenLog());
            throw new IllegalStateException("Maven build has failed, see logs for details");
        }
    }
}
