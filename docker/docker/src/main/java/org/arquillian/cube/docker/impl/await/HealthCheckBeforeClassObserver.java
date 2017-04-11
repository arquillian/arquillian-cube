package org.arquillian.cube.docker.impl.await;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.arquillian.cube.HealthCheck;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.impl.util.Timespan;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

public class HealthCheckBeforeClassObserver {

    @Inject
    Instance<DockerClientExecutor> dockerClientExecutorInstance;

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    public void executeHealthCheck(@Observes BeforeClass beforeClass) {
        final TestClass testClass = beforeClass.getTestClass();
        if (ReflectionUtil.isClassWithAnnotation(testClass.getJavaClass(), HealthCheck.class)) {

            final HealthCheck healthCheck = testClass.getAnnotation(HealthCheck.class);
            executeHealthCheck(healthCheck);

        }
    }

    private boolean executeHealthCheck(HealthCheck healthCheck) {

        final URL url = buildUrl(healthCheck);
        final int pollIterations = healthCheck.iterations();
        final long sleepTime = Timespan.toMilliseconds(healthCheck.interval());
        final int responseCode = healthCheck.responseCode();
        final String method = healthCheck.method();
        final Long connectTimeout = Timespan.toMilliseconds(healthCheck.timeout());

        return Ping.ping(pollIterations, sleepTime, TimeUnit.MILLISECONDS, () -> {
            HttpURLConnection urlConnection = null;
            try {

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(method);
                urlConnection.setConnectTimeout(connectTimeout.intValue());
                urlConnection.setReadTimeout(connectTimeout.intValue());
                urlConnection.connect();

                int connectionResponseCode = urlConnection.getResponseCode();
                if (responseCode != connectionResponseCode) {
                    return false;
                }

            } catch (IOException e) {
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return true;
        });
    }

    private URL buildUrl(HealthCheck healthCheck) {
        final String dockerServerIp = dockerClientExecutorInstance.get().getDockerServerIp();
        final String schema = healthCheck.schema();
        final int port = resolvePort(healthCheck.containerName(), healthCheck.port());
        final String context = healthCheck.value();

        return getUrlToService(schema, dockerServerIp, port, context);
    }

    private URL getUrlToService(String schema, String dockerServerIp, int port, String context) {
        try {
            return new URL(schema, dockerServerIp, port, context);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private int resolvePort(String containerName, int port) {
        if ("".equals(containerName.trim())) {
            return port;
        }

        return getBindingPort(containerName, port);
    }

    private int getBindingPort(String cubeId, int exposedPort) {

        int bindPort = -1;

        final Cube cube = getCube(cubeId);

        if (cube != null) {
            final HasPortBindings portBindings = (HasPortBindings) cube.getMetadata(HasPortBindings.class);
            final HasPortBindings.PortAddress mappedAddress = portBindings.getMappedAddress(exposedPort);

            if (mappedAddress != null) {
                bindPort = mappedAddress.getPort();
            }
        }

        return bindPort;
    }

    private Cube getCube(String cubeId) {
        return cubeRegistryInstance.get().getCube(cubeId);
    }

}
