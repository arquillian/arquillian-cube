package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.v2_6.TypedVisitor;
import io.fabric8.kubernetes.api.model.v2_6.ContainerBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class DockerRegistryVisitor extends TypedVisitor<ContainerBuilder> {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("^(.+?)(?::([^:/]+))?$");
    private static final String SPLIT_REGEX = "\\s*/\\s*";
    private static final String DOT = ".";
    private static final String COLN = ":";
    private static final String SEPARATOR = "/";

    @Inject
    Instance<Configuration> configuration;

    /**
     * Checks to see if there's a registry name already provided in the image name
     * <p>
     * Code influenced from <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/src/main/java/org/jolokia/docker/maven/util/ImageName.java">docker-maven-plugin</a>
     *
     * @return true if the image name contains a registry
     */
    public static boolean hasRegistry(String imageName) {
        if (imageName == null) {
            throw new NullPointerException("Image name must not be null");
        }

        Matcher matcher = IMAGE_PATTERN.matcher(imageName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(imageName + " is not a proper image name ([registry/][repo][:port]");
        }

        String rest = matcher.group(1);
        String[] parts = rest.split(SPLIT_REGEX);
        String part = parts[0];

        return part.contains(DOT) || part.contains(COLN);
    }

    @Override
    public void visit(ContainerBuilder containerBuilder) {
        String registry = configuration.get().getDockerRegistry();
        if (Strings.isNotNullOrEmpty(registry) && !hasRegistry(containerBuilder.getImage())) {
            containerBuilder.withImage(registry + SEPARATOR + containerBuilder.getImage());
        }
    }
}
