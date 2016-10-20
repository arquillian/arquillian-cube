package org.arquillian.cube.docker.impl.await;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.net.DownloadTool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AwaitStrategyTest {

    @Mock
    private Cube<?> cube;
    @Mock
    private DockerClientExecutor dockerClientExecutor;
    
    @Test
    public void should_be_able_to_create_http_await_strategy() {
        String containerDefinition = "tomcat:\n" +
                "            image: tutum/tomcat:7.0\n" +
                "            exposedPorts: [8089/tcp]\n" +
                "            await:\n" +
                "              strategy: http\n" +
                "              iterations: 10\n" +
                "              sleepPollingTime: 200 s\n" +
                "              url: http://localhost:8080/ping\n" +
                "              responseCode: 201\n" +
                "              match: 'Server startup'\n" +
                "              headers: \n" +
                "                   X-Cube: Docker\n";

        final DockerCompositions load = ConfigUtil.load(new ByteArrayInputStream(containerDefinition.getBytes()));
        final CubeContainer tomcat = load.getContainers().get("tomcat");

        HttpAwaitStrategy awaitStrategy = new HttpAwaitStrategy(cube, dockerClientExecutor, tomcat.getAwait());
        assertThat(awaitStrategy.getPollIterations(), is(10));
        assertThat(awaitStrategy.getUrl(), is("http://localhost:8080/ping"));
        assertThat(awaitStrategy.getResponseCode(), is(201));
        assertThat(awaitStrategy.getMatcher(), is("Server startup"));

        assertThat((String)awaitStrategy.getHeaders().get("X-Cube"), is("Docker"));
    }

    @Test
    public void should_be_able_to_create_custom_await_strategies() {

        Await await = new Await();
        await.setStrategy("org.arquillian.cube.docker.impl.await.CustomAwaitStrategyImpl");

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(dockerClientExecutor, cube, cubeContainer);

        assertThat(strategy, instanceOf(CustomAwaitStrategyInstantiator.class));
        CustomAwaitStrategyInstantiator customAwaitStrategyInstantiator = (CustomAwaitStrategyInstantiator) strategy;

        assertThat(customAwaitStrategyInstantiator.await(), is(true));
    }

    @Test
    public void should_create_static_await_strategy() {

        Await await = new Await();
        await.setStrategy("static");
        await.setIp("localhost");
        await.setPorts(Arrays.asList(8080,8089));

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(StaticAwaitStrategy.class));
        StaticAwaitStrategy staticAwaitStrategy = (StaticAwaitStrategy)strategy;

        assertThat(staticAwaitStrategy.getIp(), is("localhost"));
        assertThat(staticAwaitStrategy.getPorts().get(0), is(8080));
        assertThat(staticAwaitStrategy.getPorts().get(1), is(8089));
    }

    @Test
    public void should_create_static_await_strategy_without_defaults() {

        Await await = new Await();
        await.setStrategy("static");
        await.setIp("localhost");
        await.setPorts(Arrays.asList(8080,8089));
        await.setSleepPollingTime(200);
        await.setIterations(3);

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(StaticAwaitStrategy.class));
        StaticAwaitStrategy staticAwaitStrategy = (StaticAwaitStrategy)strategy;

        assertThat(staticAwaitStrategy.getIp(), is("localhost"));
        assertThat(staticAwaitStrategy.getPorts().get(0), is(8080));
        assertThat(staticAwaitStrategy.getPorts().get(1), is(8089));
        assertThat(staticAwaitStrategy.getPollIterations(), is(3));
        assertThat(staticAwaitStrategy.getSleepTime(), is(200));
    }

    @Test
    public void should_create_static_await_strategy_without_defaults_and_units() {

        Await await = new Await();
        await.setStrategy("static");
        await.setIp("localhost");
        await.setPorts(Arrays.asList(8080,8089));
        await.setSleepPollingTime("200 s");
        await.setIterations(3);

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(StaticAwaitStrategy.class));
        StaticAwaitStrategy staticAwaitStrategy = (StaticAwaitStrategy)strategy;

        assertThat(staticAwaitStrategy.getIp(), is("localhost"));
        assertThat(staticAwaitStrategy.getPorts().get(0), is(8080));
        assertThat(staticAwaitStrategy.getPorts().get(1), is(8089));
        assertThat(staticAwaitStrategy.getPollIterations(), is(3));
        assertThat(staticAwaitStrategy.getSleepTime(), is(200));
        assertThat(staticAwaitStrategy.getTimeUnit(), is(TimeUnit.SECONDS));
    }

    @Test
    public void should_create_polling_await_strategy_if_no_strategy_is_provided() {

        CubeContainer cubeContainer = new CubeContainer();

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
    }

    @Test
    public void should_create_polling_await_strategy() {

        Await await = new Await();
        await.setStrategy("polling");

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_port() {

        Await await = new Await();
        await.setStrategy("polling");
        await.setPorts(Arrays.asList(80));
        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getPorts(), hasItems(80));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_times() {

        Await await = new Await();
        await.setStrategy("polling");
        await.setSleepPollingTime(200);
        await.setIterations(3);

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getPollIterations(), is(3));
        assertThat(((PollingAwaitStrategy)strategy).getSleepTime(), is(200));
    }

    @Test
    public void should_create_sleeping_await_strategy_with_specific_times() {

        Await await = new Await();
        await.setStrategy("sleeping");
        await.setSleepTime("200 s");

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(SleepingAwaitStrategy.class));
        assertThat(((SleepingAwaitStrategy)strategy).getSleepTime(), is(200));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_times_and_unit() {

        Await await = new Await();
        await.setStrategy("polling");
        await.setSleepPollingTime("200 s");
        await.setIterations(3);

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getPollIterations(), is(3));
        assertThat(((PollingAwaitStrategy)strategy).getSleepTime(), is(200));
        assertThat(((PollingAwaitStrategy)strategy).getTimeUnit(), is(TimeUnit.SECONDS));
        assertThat(((PollingAwaitStrategy)strategy).getType(), is("sscommand"));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_type() {

        Await await = new Await();
        await.setStrategy("polling");
        await.setType("sscommand");
        await.setSleepPollingTime("200 s");
        await.setIterations(3);

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getType(), is("sscommand"));
    }

    @Test
    public void should_create_native_await_strategy() {

        Await await = new Await();
        await.setStrategy("native");

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(NativeAwaitStrategy.class));
    }

    @Test
    public void should_create_log_scanning_await_strategy() {

        Await await = new Await();
        await.setStrategy("log");
        await.setMatch("STARTED");

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(LogScanningAwaitStrategy.class));
        assertThat(((LogScanningAwaitStrategy)strategy).getSleepTime(), is(500));
        assertThat(((LogScanningAwaitStrategy)strategy).getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat(((LogScanningAwaitStrategy)strategy).getPollIterations(), is(10));
        assertThat(((LogScanningAwaitStrategy)strategy).isStdOut(), is(true));
        assertThat(((LogScanningAwaitStrategy)strategy).isStdErr(), is(false));
    }

    @Test
    public void should_create_log_scanning_await_strategy_without_defaults() {
        
        Await await = new Await();
        await.setStrategy("log");
        await.setMatch("regexp:.*STARTED.*");
        await.setStdOut(false);
        await.setStdErr(true);
        await.setIterations(30);
        await.setSleepPollingTime("2s");

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setAwait(await);

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, cubeContainer);

        assertThat(strategy, instanceOf(LogScanningAwaitStrategy.class));
        assertThat(((LogScanningAwaitStrategy)strategy).getSleepTime(), is(2));
        assertThat(((LogScanningAwaitStrategy)strategy).getTimeUnit(), is(TimeUnit.SECONDS));
        assertThat(((LogScanningAwaitStrategy)strategy).getPollIterations(), is(30));
        assertThat(((LogScanningAwaitStrategy)strategy).isStdOut(), is(false));
        assertThat(((LogScanningAwaitStrategy)strategy).isStdErr(), is(true));
    }

}
