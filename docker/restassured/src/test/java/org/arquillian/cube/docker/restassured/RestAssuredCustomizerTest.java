package org.arquillian.cube.docker.restassured;

import io.restassured.RestAssured;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.jboss.arquillian.core.api.Instance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RestAssuredCustomizerTest {

    private static final String SIMPLE_SCENARIO =
            "helloworld:\n" +
                    "  image: dockercloud/hello-world\n" +
                    "  portBindings: [8080->80/tcp]";

    @Mock
    CubeDockerConfiguration cubeDockerConfiguration;

    @Before
    public void setup() {
        when(cubeDockerConfiguration.getDockerServerIp()).thenReturn("192.168.99.100");
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(ConfigUtil.load(SIMPLE_SCENARIO));
    }

    @Test
    public void should_autoresolve_base_uri() {
        RestAssuredCustomizer restAssuredCustomizer = new RestAssuredCustomizer();
        restAssuredCustomizer.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };

        Map<String, String> conf = new HashMap<>();

        final RestAssuredConfiguration restAssuredConfiguration = RestAssuredConfiguration.fromMap(conf);
        restAssuredCustomizer.configure(restAssuredConfiguration);

        assertThat(RestAssured.baseURI).isEqualTo("http://192.168.99.100");
    }

    @Test
    public void should_autoresolve_base_uri_with_schema_if_set() {
        RestAssuredCustomizer restAssuredCustomizer = new RestAssuredCustomizer();
        restAssuredCustomizer.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };

        Map<String, String> conf = new HashMap<>();
        conf.put("schema", "https");

        final RestAssuredConfiguration restAssuredConfiguration = RestAssuredConfiguration.fromMap(conf);
        restAssuredCustomizer.configure(restAssuredConfiguration);

        assertThat(RestAssured.baseURI).isEqualTo("https://192.168.99.100");
    }

    @Test
    public void should_use_base_uri() {
        RestAssuredCustomizer restAssuredCustomizer = new RestAssuredCustomizer();
        restAssuredCustomizer.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };

        Map<String, String> conf = new HashMap<>();
        conf.put("baseUri", "http://localhost");

        final RestAssuredConfiguration restAssuredConfiguration = RestAssuredConfiguration.fromMap(conf);
        restAssuredCustomizer.configure(restAssuredConfiguration);

        assertThat(RestAssured.baseURI).isEqualTo("http://localhost");
    }

    @Test
    public void should_autoresolve_port() {
        RestAssuredCustomizer restAssuredCustomizer = new RestAssuredCustomizer();
        restAssuredCustomizer.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };

        Map<String, String> conf = new HashMap<>();
        conf.put("baseUri", "http://localhost");

        final RestAssuredConfiguration restAssuredConfiguration = RestAssuredConfiguration.fromMap(conf);
        restAssuredCustomizer.configure(restAssuredConfiguration);

        assertThat(RestAssured.port).isEqualTo(8080);
    }

    @Test
    public void should_resolve_exposed_port() {
        RestAssuredCustomizer restAssuredCustomizer = new RestAssuredCustomizer();
        restAssuredCustomizer.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };

        Map<String, String> conf = new HashMap<>();
        conf.put("baseUri", "http://localhost");
        conf.put("port", "80");

        final RestAssuredConfiguration restAssuredConfiguration = RestAssuredConfiguration.fromMap(conf);
        restAssuredCustomizer.configure(restAssuredConfiguration);

        assertThat(RestAssured.port).isEqualTo(8080);
    }

    @Test
    public void should_resolve_exposed_port_as_bind_port_if_no_definitions() {
        RestAssuredCustomizer restAssuredCustomizer = new RestAssuredCustomizer();
        restAssuredCustomizer.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };

        Map<String, String> conf = new HashMap<>();
        conf.put("baseUri", "http://localhost");
        conf.put("port", "8081");

        final RestAssuredConfiguration restAssuredConfiguration = RestAssuredConfiguration.fromMap(conf);
        restAssuredCustomizer.configure(restAssuredConfiguration);

        assertThat(RestAssured.port).isEqualTo(8081);
    }

}
