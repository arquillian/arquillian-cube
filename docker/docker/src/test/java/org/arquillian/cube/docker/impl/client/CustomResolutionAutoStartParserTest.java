package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.arquillian.cube.spi.AutoStartParser;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class CustomResolutionAutoStartParserTest extends AbstractManagerTestBase {

    private static final String CONTENT =
        "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]\n" +
            "go:\n" +
            "  image: tutum/tomcat:7.0\n";

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        super.addExtensions(extensions);
    }

    @Test
    public void shouldInstantiateACustomAutoStartParser() {

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", CONTENT);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        final CubeDockerConfiguration cubeDockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        Injector injector = new Injector() {
            @Override
            public <T> T inject(T target) {
                final ChangeNameAutoStartParser orderByNameAutoStartParser = (ChangeNameAutoStartParser) target;
                orderByNameAutoStartParser.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
                    @Override
                    public CubeDockerConfiguration get() {
                        return cubeDockerConfiguration;
                    }
                };
                return target;
            }
        };

        final AutoStartParser autoStartParser =
            AutoStartParserFactory.create("custom:org.arquillian.cube.docker.impl.client.ChangeNameAutoStartParser", null,
                injector);
        assertThat(autoStartParser, instanceOf(CustomAutoStartParser.class));

        final Set<String> names = autoStartParser.parse().keySet();
        assertThat(names, hasItem("og"));
        assertThat(names, hasItem("tacmot"));
    }
}
