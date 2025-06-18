package org.eclipse.jkube.maven.plugin.build;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.pom.equipped.ConfigurationDistributionStage;

public class JKubeMavenPluginResourceGeneratorBuilder {

    private static final Logger logger = Logger.getLogger(JKubeMavenPluginResourceGeneratorBuilder.class.getName());

    private Path pom;
    private String[] k8sGoals = new String[] {"package", "k8s:build", "k8s:resource", "k8s:push", "k8s:apply"};
    private String[] openshiftGoals = new String[] {"package", "oc:build", "oc:resource"};
    private String namespace;
    private boolean mvnDebugOutput;
    private boolean quietMode;
    private String mavenOpts;
    private String[] profiles = new String[0];
    private Map<String, String> properties = new HashMap<>();
    private boolean forOpenshift = false;
    private boolean useCustomMaven = false;


    public JKubeMavenPluginResourceGeneratorBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder k8sGoals(String[] goals) {
        this.k8sGoals = goals;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder openshiftGoals(String[] goals) {
        this.openshiftGoals = goals;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder pluginConfigurationIn(Path pom) {
        this.pom = pom;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder quiet() {
        return quiet(true);
    }

    public JKubeMavenPluginResourceGeneratorBuilder quiet(boolean quiet) {
        this.quietMode = quiet;
        return this;
    }

    /**
     * Enables mvn debug output (-X) flag. Implies build logging output.
     */
    private JKubeMavenPluginResourceGeneratorBuilder withDebugOutput(boolean debug) {
        this.mvnDebugOutput = debug;
        quiet(!debug);
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder debug(boolean debug) {
        this.mvnDebugOutput = debug;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder addMavenOpts(String options) {
        this.mavenOpts = options;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder profiles(List<String> profiles) {
        return profiles(profiles.toArray(new String[profiles.size()]));
    }

    public JKubeMavenPluginResourceGeneratorBuilder profiles(String... profiles) {
        this.profiles = profiles;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder withProperties(List<String> propertiesPairs) {
        return withProperties(propertiesPairs.toArray(new String[propertiesPairs.size()]));
    }

    public JKubeMavenPluginResourceGeneratorBuilder withProperties(String... propertiesPairs) {
        if (propertiesPairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                String.format("Expecting even amount of variable name - value pairs to be passed. Got %s entries. %s", propertiesPairs.length, Arrays.toString(propertiesPairs)));
        }

        for (int i = 0; i < propertiesPairs.length; i += 2) {
            this.properties.put(propertiesPairs[i], propertiesPairs[i + 1]);
        }

        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder forOpenshift(boolean forOpenshift) {
        this.forOpenshift = forOpenshift;
        return this;
    }

    public JKubeMavenPluginResourceGeneratorBuilder withMaven(final boolean useCustomMaven) {
        this.useCustomMaven = useCustomMaven;
        return this;
    }


    public void build() {
        final ConfigurationDistributionStage distributionStage = EmbeddedMaven
            .forProject(pom.toFile())
            .setQuiet(quietMode)
            .setDebug(mvnDebugOutput)
            .setDebugLoggerLevel()
            .setGoals(forOpenshift ? openshiftGoals : k8sGoals)
            .addProperty("jkube.namespace", namespace);
        if (useCustomMaven) {
            distributionStage.useLocalInstallation();
        } else {
            distributionStage.useDefaultDistribution();
        }
        this.build(distributionStage);
    }

    public void build(ConfigurationDistributionStage distributionStage) {
        // TODO: https://github.com/arquillian/arquillian-cube/issues/1017
        if (System.getenv("JAVA_HOME") == null) {
            logger.warning(
                "No JAVA_HOME defined. Defaulting to /usr/lib/jvm/java-1.8.0. If that's not where it should be, "
                    + "please define JAVA_HOME environment variable and re-run this test. See https://git.io/vxWo9 for reasons.");
            distributionStage.addShellEnvironment("JAVA_HOME", "/usr/lib/jvm/java-1.8.0");
        }

        if (profiles.length > 0) {
            distributionStage.setProfiles(profiles);
        }

        if (!properties.isEmpty()) {
            distributionStage.setProperties(asProperties(properties));
        }

        if (mavenOpts != null && !mavenOpts.isEmpty()) {
            distributionStage.setMavenOpts(mavenOpts);
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
