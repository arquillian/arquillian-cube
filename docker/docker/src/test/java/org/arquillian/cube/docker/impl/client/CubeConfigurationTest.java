package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import org.hamcrest.collection.IsIterableContainingInOrder;
import static org.junit.Assert.assertThat;

public class CubeConfigurationTest {

    private static final String CONTENT =
        "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    private static final String CONTENT2 =
        "tomcat2:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    private static final String DOCKER_COMPOSE_CONTENT =
        "web:\n" +
            "  build: .\n" +
            "  ports:\n" +
            "   - \"5000:5000\"\n" +
            "  volumes:\n" +
            "   - .:/code\n" +
            "  links:\n" +
            "   - redis\n" +
            "redis:\n" +
            "  image: redis";

    private static final String OVERRIDE_CUSTOM =
        "tomcat:\n" +
            "  image: tutum/tomcat:8.0\n" +
            "  await:\n" +
            "    strategy: polling\n" +
            "  afterStart: \n" +
            "    - copy:\n" +
            "        from: /tmp\n" +
            "        to: /test\n" +
            "  beforeStop: \n" +
            "    - copy:\n" +
            "        from: /test\n" +
            "        to: /tmp\n" +
            "  afterStop: \n" +
            "    - copy:\n" +
            "        from: /test\n" +
            "        to: /tmp";

    private static final String VERSION_2_WITH_VOLUMES = "version: '2'\n" +
        "services:\n" +
        "  nginx:\n" +
        "    image: \"nginx:alpine\"\n" +
        "    ports:\n" +
        "    - \"80\"\n" +
        "    volumes:\n" +
        "    - \"/tmp/www:/usr/share/nginx/html\"";

    private static final String VERSION_2_WITH_PORT_RANGE = "version: '2'\n" +
        "services:\n" +
        "  nginx:\n" +
        "    image: \"nginx:alpine\"\n" +
        "    ports:\n" +
        "    - \"80-84:90-94\"\n" +
        "    volumes:\n" +
        "    - \"/tmp/www:/usr/share/nginx/html\"";

    private static final String VERSION_2_WITH_SPACE_SEPERATED_COMMAND = "version: '2'\n"
            + "services:\n"
            + "  wildfly:\n"
            + "    image: \"jboss/wildfly\"\n"
            + "    command: /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0";

    private static final String VERSION_2_WITH_SPACE_AND_QUOTES_SEPERATED_COMMAND = "version: '2'\n"
            + "services:\n"
            + "  wildfly:\n"
            + "    image: \"jboss/wildfly\"\n"
            + "    command: \"/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0\"";

    private static final String VERSION_2_WITH_ARRAY_COMMAND = "version: '2'\n"
            + "services:\n"
            + "  wildfly:\n"
            + "    image: \"jboss/wildfly\"\n"
            + "    command: [\"/opt/jboss/wildfly/bin/standalone.sh\", \"-b\", \"0.0.0.0\", \"-bmanagement\", \"0.0.0.0\"]";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void shouldSplitCmdOnSpaces() {
        testCmdSplittedOnSpaces(VERSION_2_WITH_SPACE_SEPERATED_COMMAND, new String[]{"/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"});
    }

    @Test
    public void shouldSplitCmdWihtQuotesOnSpaces() {
        testCmdSplittedOnSpaces(VERSION_2_WITH_SPACE_AND_QUOTES_SEPERATED_COMMAND, new String[]{"/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"});
    }

    @Test
    public void shouldSplitCmdWithArray() {
        testCmdSplittedOnSpaces(VERSION_2_WITH_ARRAY_COMMAND, new String[]{"/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"});
    }

    private void testCmdSplittedOnSpaces(final String composeDefinition, final String[] expectedInOrderCmds) {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", composeDefinition);
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        final CubeContainer wildfly = dockerContainersContent.get("wildfly");
        final Collection<String> commands = wildfly.getCmd();

        assertThat(commands, IsIterableContainingInOrder.contains(expectedInOrderCmds));
    }

    @Test
    public void should_expand_ports_from_docker_compose_version_2() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", VERSION_2_WITH_PORT_RANGE);
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        final CubeContainer ngnix = dockerContainersContent.get("nginx");
        final Collection<PortBinding> portBindings = ngnix.getPortBindings();

        assertThat(portBindings, containsInAnyOrder(PortBinding.valueOf("80->90"), PortBinding.valueOf("81->91"),
            PortBinding.valueOf("82->92"), PortBinding.valueOf("83->93"), PortBinding.valueOf("84->94")));
    }

    @Test
    public void should_load_volumes_from_docker_compose_version_2() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", VERSION_2_WITH_VOLUMES);
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        final CubeContainer ngnix = dockerContainersContent.get("nginx");
        final Collection<String> volumes = ngnix.getVolumes();
        final String volume = volumes.iterator().next();

        assertThat(volume, is("/tmp/www:/usr/share/nginx/html"));

        final Collection<String> binds = ngnix.getBinds();
        final String bind = volumes.iterator().next();

        assertThat(bind, is("/tmp/www:/usr/share/nginx/html"));
    }

    @Test
    public void shouldChangeNamesInParallelizeStarCubes() {
        String content =
            "tomcat*:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping*\n" +
                "ping*:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeDockerConfigurator cubeDockerConfigurator = new CubeDockerConfigurator();
        final CubeDockerConfiguration cubeDockerConfiguration =
            cubeDockerConfigurator.resolveDynamicNames(cubeConfiguration);

        final Set<String> containerIds = cubeDockerConfiguration.getDockerContainersContent().getContainerIds();
        final String tomcat = findElementStartingWith(containerIds, "tomcat");
        assertThat(tomcat.length(), is(greaterThan(6)));

        final String ping = findElementStartingWith(containerIds, "ping");
        assertThat(ping.length(), is(greaterThan(4)));
    }

    @Test
    public void shouldAddEnvVarsWithHostNameInParallelizeStarCubes() {
        String content =
            "tomcat*:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping*\n" +
                "ping*:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeDockerConfigurator cubeDockerConfigurator = new CubeDockerConfigurator();
        final CubeDockerConfiguration cubeDockerConfiguration =
            cubeDockerConfigurator.resolveDynamicNames(cubeConfiguration);

        final Set<String> containerIds = cubeDockerConfiguration.getDockerContainersContent().getContainerIds();

        final String tomcat = findElementStartingWith(containerIds, "tomcat");
        final String ping = findElementStartingWith(containerIds, "ping");

        final CubeContainer tomcatContainer = cubeDockerConfiguration.getDockerContainersContent().get(tomcat);
        assertThat(getFirst(tomcatContainer.getEnv()), is("PING_HOSTNAME=" + ping));
    }

    @Test
    public void shouldChangePortBindingToPrivatePortsInParallelizeStarCubes() {
        String content =
            "tomcat*:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping*\n" +
                "ping*:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeDockerConfigurator cubeDockerConfigurator = new CubeDockerConfigurator();
        final CubeDockerConfiguration cubeDockerConfiguration =
            cubeDockerConfigurator.resolveDynamicNames(cubeConfiguration);

        final Set<String> containerIds = cubeDockerConfiguration.getDockerContainersContent().getContainerIds();
        final String tomcat = findElementStartingWith(containerIds, "tomcat");

        final CubeContainer tomcatContainer = cubeDockerConfiguration.getDockerContainersContent().get(tomcat);
        assertThat(getFirst(tomcatContainer.getPortBindings()).getBound(), is(greaterThan(49152)));
    }

    @Test
    public void shouldChangeStarLinksInParallelizeStarCubes() {
        String content =
            "tomcat*:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping*\n" +
                "ping*:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeDockerConfigurator cubeDockerConfigurator = new CubeDockerConfigurator();
        final CubeDockerConfiguration cubeDockerConfiguration =
            cubeDockerConfigurator.resolveDynamicNames(cubeConfiguration);

        final Set<String> containerIds = cubeDockerConfiguration.getDockerContainersContent().getContainerIds();

        final String tomcat = findElementStartingWith(containerIds, "tomcat");
        final String ping = findElementStartingWith(containerIds, "ping");

        final CubeContainer tomcatContainer = cubeDockerConfiguration.getDockerContainersContent().get(tomcat);
        assertThat(getFirst(tomcatContainer.getLinks()).getName(), is(ping));
    }

    @Test
    public void shouldParallelizeStarCubesUsingRemappingAlias() {
        String content =
            "tomcat*:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping*:bb\n" +
                "ping*:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeDockerConfigurator cubeDockerConfigurator = new CubeDockerConfigurator();
        final CubeDockerConfiguration cubeDockerConfiguration =
            cubeDockerConfigurator.resolveDynamicNames(cubeConfiguration);

        final Set<String> containerIds = cubeDockerConfiguration.getDockerContainersContent().getContainerIds();
        final String tomcat = findElementStartingWith(containerIds, "tomcat");

        final String ping = findElementStartingWith(containerIds, "ping");

        String uuid = ping.substring(ping.indexOf('_') + 1);

        final CubeContainer tomcatContainer = cubeDockerConfiguration.getDockerContainersContent().get(tomcat);
        Link link = getFirst(tomcatContainer.getLinks());
        assertThat(link.getAlias(), is("bb_" + uuid));
    }

    @Test
    public void shouldParallelizeNetworks() {
        String content =
            "networks:\n" +
            "  network1*:\n" +
            "    driver: bridge\n" +
            "  network2:\n" +
            "    driver: bridge\n" +
            "  network3*:\n" +
            "    driver: bridge\n" +
            "tomcat:\n" +
            "  image: tutum/tomcat:8.0\n" +
            "  networkMode: network3*\n" +
            "ping:\n" +
            "  image: hashicorp/http-echo\n" +
            "  networks:\n" +
            "    - network1*\n" +
            "    - network2\n";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeDockerConfigurator cubeDockerConfigurator = new CubeDockerConfigurator();
        final CubeDockerConfiguration cubeDockerConfiguration =
            cubeDockerConfigurator.resolveDynamicNames(cubeConfiguration);

        final DockerCompositions dockerContainersContent = cubeDockerConfiguration.getDockerContainersContent();
        final Set<String> networkIds = dockerContainersContent.getNetworkIds();
        String network1 = findElementStartingWith(networkIds, "network1");
        assertThat(network1, is(not("network1*")));
        String network2 = findElementStartingWith(networkIds, "network2");
        assertThat(network2, is("network2"));
        String network3 = findElementStartingWith(networkIds, "network3");
        assertThat(network3, is(not("network3*")));

        final CubeContainer tomcat = dockerContainersContent.get("tomcat");
        assertThat(tomcat.getNetworkMode(), is(network3));

        final CubeContainer ping = dockerContainersContent.get("ping");
        assertThat(ping.getNetworks(), containsInAnyOrder(network1, network2));
    }

    private <T> T getFirst(Collection<T> collection) {
        return collection.iterator().next();
    }

    private String findElementStartingWith(Set<String> elements, String startsWith) {
        for (String element : elements) {
            if (element.startsWith(startsWith)) {
                return element;
            }
        }

        return null;
    }

    @Test
    public void should_override_custom_cube_properties() throws IOException {
        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFiles", newFile.toURI().toString());
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("cubeSpecificProperties", OVERRIDE_CUSTOM);

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        final CubeContainer tomcat = dockerContainersContent.get("tomcat");
        assertThat(tomcat, is(notNullValue()));
        assertThat(tomcat.getImage().getTag(), is("7.0"));
        assertThat(tomcat.getAwait().getStrategy(), is("polling"));
        assertThat(tomcat.getAfterStart().size(), is(1));
        assertThat(tomcat.getBeforeStop().size(), is(1));
    }

    @Test
    public void should_merge_more_than_one_file_into_one() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        File newFile2 = testFolder.newFile("config2.yaml");
        Files.write(Paths.get(newFile2.toURI()), CONTENT2.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFiles", newFile.toURI().toString() + ", " + newFile2.toURI().toString());
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        assertThat(dockerContainersContent.get("tomcat"), is(notNullValue()));
        assertThat(dockerContainersContent.get("tomcat2"), is(notNullValue()));
    }

    @Test
    public void should_load_docker_compose_format() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", DOCKER_COMPOSE_CONTENT);
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        CubeContainer actualWeb = dockerContainersContent.get("web");
        assertThat(actualWeb.getBuildImage(), is(notNullValue()));
        assertThat(actualWeb.getPortBindings(), is(notNullValue()));
        assertThat(actualWeb.getVolumes(), is(notNullValue()));
        assertThat(actualWeb.getLinks(), is(notNullValue()));

        CubeContainer actualRedis = dockerContainersContent.get("redis");
        assertThat(actualRedis.getImage(), is(notNullValue()));
    }

    @Test
    public void should_load_cube_configuration_from_cube_file_if_no_file_is_provided() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tomcat:7.0"));
    }

    @Test
    public void should_parse_and_load_configuration_file() {

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("dockerContainers", CONTENT);

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
    }

    @Test
    public void should_parse_and_load_configuration_file_from_container_configuration_file_and_property_set_file()
        throws IOException {

        File newFile = testFolder.newFile("config.yml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        File newFile2 = testFolder.newFile("config.demo.yml");
        Files.write(Paths.get(newFile2.toURI()), CONTENT2.getBytes());
        System.setProperty("cube.environment", "demo");
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("dockerContainersFile", newFile.toURI().toString());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
        assertThat(dockerContainersContent.get("tomcat2"), is(notNullValue()));
    }

    @Test
    public void should_parse_and_load_configuration_file_from_container_configuration_file() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("dockerContainersFile", newFile.toURI().toString());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
    }

    @Test
    public void should_parse_and_load_configuration_from_container_configuration_resource() throws IOException {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("dockerContainersResource", "test-topologies/topology1.yaml");

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
    }

    @Test
    public void should_be_able_to_extend_and_override_toplevel() throws Exception {
        String config =
            "tomcat6:\n" +
                "  image: tutum/tomcat:6.0\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "  await:\n" +
                "    strategy: static\n" +
                "    ip: localhost\n" +
                "    ports: [8080, 8089]\n" +
                "tomcat7:\n" +
                "  extends: tomcat6\n" +
                "  image: tutum/tomcat:7.0\n";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeContainer tomcat7 = cubeConfiguration.getDockerContainersContent().get("tomcat7");
        Assert.assertEquals("tutum/tomcat:7.0", tomcat7.getImage().toImageRef());
        Assert.assertTrue(tomcat7.getAwait() != null);
        Assert.assertEquals("8089/tcp", tomcat7.getExposedPorts().iterator().next().toString());
    }

    @Test
    public void should_be_able_to_read_network_configuration() {
        String config =
            "networks:\n" +
                "  mynetwork:\n " +
                "    driver: bridge\n" +
                "tomcat6:\n" +
                "  image: tutum/tomcat:6.0\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "  await:\n" +
                "    strategy: static\n" +
                "    ip: localhost\n" +
                "    ports: [8080, 8089]\n" +
                "tomcat7:\n" +
                "  extends: tomcat6\n" +
                "  image: tutum/tomcat:7.0\n";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final Network mynetwork = cubeConfiguration.getDockerContainersContent().getNetwork("mynetwork");
        assertThat(mynetwork, is(notNullValue()));
        assertThat(mynetwork.getDriver(), is("bridge"));
    }

    @Test
    public void should_set_global_removeVolumes_option_if_not_set_on_container_level() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", CONTENT);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        CubeContainer containerConfig = cubeConfiguration.getDockerContainersContent().get("tomcat");
        assertThat(containerConfig.getRemoveVolumes(), is(true));

        parameters.put(CubeDockerConfiguration.REMOVE_VOLUMES, "true");
        cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        containerConfig = cubeConfiguration.getDockerContainersContent().get("tomcat");
        assertThat(containerConfig.getRemoveVolumes(), is(true));
    }

    @Test
    public void should_container_level_removeVolumes_option_overwrite_global_value() {
        String config1 =
            "tomcat:\n" +
                "  image: tutum/tomcat:6.0\n" +
                "  removeVolumes: false";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(CubeDockerConfiguration.REMOVE_VOLUMES, "true");
        parameters.put("dockerContainers", config1);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        CubeContainer containerConfig = cubeConfiguration.getDockerContainersContent().get("tomcat");
        assertThat(containerConfig.getRemoveVolumes(), is(false));

        String config2 =
            "tomcat:\n" +
                "  image: tutum/tomcat:6.0\n" +
                "  removeVolumes: true";

        parameters.put(CubeDockerConfiguration.REMOVE_VOLUMES, "false");
        parameters.put("dockerContainers", config2);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        containerConfig = cubeConfiguration.getDockerContainersContent().get("tomcat");
        assertThat(containerConfig.getRemoveVolumes(), is(true));
    }
}
