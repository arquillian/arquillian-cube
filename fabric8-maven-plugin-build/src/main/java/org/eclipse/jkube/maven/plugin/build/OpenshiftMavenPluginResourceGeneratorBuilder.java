package org.eclipse.jkube.maven.plugin.build;

import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.pom.equipped.ConfigurationDistributionStage;

public class OpenshiftMavenPluginResourceGeneratorBuilder extends KubernetesMavenPluginResourceGeneratorBuilder {
    private String[] goals = new String[] {"package", "oc:build", "oc:resource"};

    public void build() {
        final ConfigurationDistributionStage distributionStage = EmbeddedMaven
            .forProject(pom.toFile())
            .setQuiet(quietMode)
            .useDefaultDistribution()
            .setDebug(mvnDebugOutput)
            .setDebugLoggerLevel()
            .setGoals(goals)
            .addProperty("jkube.namespace", namespace);
        this.build(distributionStage);
    }
}
