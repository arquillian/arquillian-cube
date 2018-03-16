package org.fabric8.maven.plugin.build;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.pom.equipped.ConfigurationDistributionStage;

public class Fabric8MavenPluginResourceGeneratorBuilder {

    private Path pom;
    private String[] goals = new String[] {"package", "fabric8:build", "fabric8:resource"};
    private String namespace;
    private boolean mvnDebugOutput;
    private boolean quietMode;
    private String[] profiles;
    private Map<String, String> properties = new HashMap<>();

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

    public Fabric8MavenPluginResourceGeneratorBuilder quiet() {
        return quiet(true);
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

    public Fabric8MavenPluginResourceGeneratorBuilder profiles(List<String> profiles) {
        return profiles(profiles.toArray(new String[profiles.size()]));
    }

    public Fabric8MavenPluginResourceGeneratorBuilder profiles(String... profiles) {
        this.profiles = profiles;
        return this;
    }

    public Fabric8MavenPluginResourceGeneratorBuilder withProperties(List<String> propertiesPairs) {
        return withProperties(propertiesPairs.toArray(new String[propertiesPairs.size()]));
    }

    public Fabric8MavenPluginResourceGeneratorBuilder withProperties(String... propertiesPairs) {
        if (propertiesPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Expecting even amount of variable name - value pairs to be passed. Got "
                + propertiesPairs.length
                + " entries. "
                + Arrays.toString(propertiesPairs));
        }

        for (int i = 0; i < propertiesPairs.length; i += 2) {
            this.properties.put(propertiesPairs[i], propertiesPairs[i + 1]);
        }

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

        if (profiles.length > 0) {
            distributionStage.setProfiles(profiles);
        }

        if (properties.isEmpty()) {
            distributionStage.setProperties(asProperties(properties));
        }

        final BuiltProject builtProject = distributionStage.ignoreFailure()
            .build();

        if (builtProject.getMavenBuildExitCode() != 0) {
            System.out.println(builtProject.getMavenLog());
            throw new IllegalStateException("Maven build has failed, see logs for details");
        }
    }

    private Properties asProperties(Map<String, String> propertyMap) {
        final Properties properties = new Properties();
        properties.putAll(propertyMap);
        return properties;
    }
}
