package org.arquillian.cube.docker.impl.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.ExposedPort;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public final class ConfigUtil {

    private static final String NETWORKS = "networks";

    private static final String CONTAINERS = "containers";

    private ConfigUtil() {
    }

    public static String[] trim(String[] data) {
        List<String> result = new ArrayList<String>();
        for (String val : data) {
            String trimmed = val.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.toArray(new String[] {});
    }

    public static String[] reverse(String[] cubeIds) {
        String[] result = new String[cubeIds.length];
        int n = cubeIds.length - 1;
        for (int i = 0; i < cubeIds.length; i++) {
            result[n--] = cubeIds[i];
        }
        return result;
    }

    public static String dump(DockerCompositions containers) {
        Yaml yaml = new Yaml(new CubeRepresenter());
        return yaml.dump(containers);
    }

    public static DockerCompositions load(String content) {
        return load(new ByteArrayInputStream(content.getBytes()));
    }

    @SuppressWarnings("unchecked")
    public static DockerCompositions load(InputStream inputStream) {
        // TODO: Figure out how to map root Map<String, Type> objects. Workaround by mapping it to Map structure then dumping it into individual objects
        Yaml yaml = new Yaml(new CubeConstructor());
        Map<String, Object> rawLoad = (Map<String, Object>) yaml.load(inputStream);

        DockerCompositions containers = new DockerCompositions();

        for (Map.Entry<String, Object> rawLoadEntry : rawLoad.entrySet()) {
            if (NETWORKS.equals(rawLoadEntry.getKey())) {
                Map<String, Object> rawNetworks = (Map<String, Object>) rawLoadEntry.getValue();
                for (Map.Entry<String, Object> rawNetworkEntry : rawNetworks.entrySet()) {
                    Network network = yaml.loadAs(yaml.dump(rawNetworkEntry.getValue()), Network.class);
                    containers.add(rawNetworkEntry.getKey(), network);
                }
            } else if (CONTAINERS.equals(rawLoadEntry.getKey())) {
                Map<String, Object> rawContainers = (Map<String, Object>) rawLoadEntry.getValue();
                for (Map.Entry<String, Object> rawContainerEntry : rawContainers.entrySet()) {
                    CubeContainer cubeContainer =
                        yaml.loadAs(yaml.dump(rawContainerEntry.getValue()), CubeContainer.class);
                    containers.add(rawContainerEntry.getKey(), cubeContainer);
                }
            } else {
                CubeContainer container = yaml.loadAs(yaml.dump(rawLoadEntry.getValue()), CubeContainer.class);
                containers.add(rawLoadEntry.getKey(), container);
            }
        }
        return applyExtendsRules(containers);
    }

    private static DockerCompositions applyExtendsRules(DockerCompositions dockerCompositions) {

        for (Map.Entry<String, CubeContainer> containerEntry : dockerCompositions.getContainers().entrySet()) {
            CubeContainer container = containerEntry.getValue();
            if (container.getExtends() != null) {
                String extendsContainer = container.getExtends();
                if (dockerCompositions.get(extendsContainer) == null) {
                    throw new IllegalArgumentException(
                        containerEntry.getKey() + " extends a non existing container definition " + extendsContainer);
                }
                CubeContainer extendedContainer = dockerCompositions.get(extendsContainer);
                container.merge(extendedContainer);
            }
        }
        return dockerCompositions;
    }

    private static class CubeRepresenter extends Representer {
        public CubeRepresenter() {
            this.representers.put(PortBinding.class, new ToStringRepresent());
            this.representers.put(ExposedPort.class, new ToStringRepresent());
            this.representers.put(Image.class, new ToStringRepresent());
            this.representers.put(Link.class, new ToStringRepresent());
            addClassTag(DockerCompositions.class, Tag.MAP);
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,
            Tag customTag) {
            if (propertyValue == null) {
                return null;
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }

        public class ToStringRepresent implements Represent {
            @Override
            public Node representData(Object data) {
                return representScalar(Tag.STR, data.toString());
            }
        }
    }

    public static class CubeConstructor extends Constructor {
        public CubeConstructor() {
            this.yamlClassConstructors.put(NodeId.scalar, new CubeMapping());
        }

        private class CubeMapping extends Constructor.ConstructScalar {

            @Override
            public Object construct(Node node) {
                if (node.getType() == PortBinding.class) {
                    String value = constructScalar((ScalarNode) node).toString();
                    return PortBinding.valueOf(value);
                } else if (node.getType() == ExposedPort.class) {
                    String value = constructScalar((ScalarNode) node).toString();
                    return ExposedPort.valueOf(value);
                } else if (node.getType() == Image.class) {
                    String value = constructScalar((ScalarNode) node).toString();
                    return Image.valueOf(value);
                } else if (node.getType() == Link.class) {
                    String value = constructScalar((ScalarNode) node).toString();
                    return Link.valueOf(value);
                }
                return super.construct(node);
            }
        }
    }
}
