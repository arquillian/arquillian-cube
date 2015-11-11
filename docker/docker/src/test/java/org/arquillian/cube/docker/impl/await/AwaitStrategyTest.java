package org.arquillian.cube.docker.impl.await;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.await.AwaitStrategy;
import org.arquillian.cube.docker.impl.await.AwaitStrategyFactory;
import org.arquillian.cube.docker.impl.await.NativeAwaitStrategy;
import org.arquillian.cube.docker.impl.await.PollingAwaitStrategy;
import org.arquillian.cube.docker.impl.await.SleepingAwaitStrategy;
import org.arquillian.cube.docker.impl.await.StaticAwaitStrategy;
import org.arquillian.cube.spi.Cube;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.yaml.snakeyaml.Yaml;

@RunWith(MockitoJUnitRunner.class)
public class AwaitStrategyTest {

    private static final String CONTENT_WITH_STATIC_STRATEGY = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080,8089]";

    private static final String CONTENT_WITH_STATIC_STRATEGY_WITHOUT_DEFAULTS = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080,8089]\n" +
            "    sleepPollingTime: 200\n" +
            "    iterations: 3";

    private static final String CONTENT_WITH_STATIC_STRATEGY_WITHOUT_DEFAULTS_UNIT = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080,8089]\n" +
            "    sleepPollingTime: 200 s\n" +
            "    iterations: 3";

    private static final String CONTENT_WITH_NO_STRATEGY = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n";

    private static final String CONTENT_WITH_POLLING_STRATEGY = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: polling";

    private static final String CONTENT_WITH_POLLING_STRATEGY_WITHOUT_DEFAULTS = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: polling\n" +
            "    sleepPollingTime: 200\n" +
            "    iterations: 3";

    private static final String CONTENT_WITH_NATIVE_STRATEGY = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: native";


    private static final String CONTENT_WITH_SLEEPING_STRATEGY_WITHOUT_DEFAULTS_AND_UNIT = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: sleeping\n" +
            "    sleepTime: 200 s\n";

    private static final String CONTENT_WITH_POLLING_STRATEGY_WITHOUT_DEFAULTS_AND_UNIT = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: polling\n" +
            "    sleepPollingTime: 200 s\n" +
            "    iterations: 3";

    private static final String CONTENT_WITH_SSCOMMAND_STRATEGY = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: polling\n" +
            "    sleepPollingTime: 200 s\n" +
            "    iterations: 3\n" +
            "    type: sscommand";

    @Mock
    private Cube cube;

    @Test
    public void should_create_static_await_strategy() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_STATIC_STRATEGY);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(StaticAwaitStrategy.class));
        StaticAwaitStrategy staticAwaitStrategy = (StaticAwaitStrategy)strategy;

        assertThat(staticAwaitStrategy.getIp(), is("localhost"));
        assertThat(staticAwaitStrategy.getPorts().get(0), is(8080));
        assertThat(staticAwaitStrategy.getPorts().get(1), is(8089));
    }

    @Test
    public void should_create_static_await_strategy_without_defaults() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_STATIC_STRATEGY_WITHOUT_DEFAULTS);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(StaticAwaitStrategy.class));
        StaticAwaitStrategy staticAwaitStrategy = (StaticAwaitStrategy)strategy;

        assertThat(staticAwaitStrategy.getIp(), is("localhost"));
        assertThat(staticAwaitStrategy.getPorts().get(0), is(8080));
        assertThat(staticAwaitStrategy.getPorts().get(1), is(8089));
        assertThat(staticAwaitStrategy.getPollIterations(), is(3));
        assertThat(staticAwaitStrategy.getSleepPollTime(), is(200));
    }

    @Test
    public void should_create_static_await_strategy_without_defaults_and_units() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_STATIC_STRATEGY_WITHOUT_DEFAULTS_UNIT);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(StaticAwaitStrategy.class));
        StaticAwaitStrategy staticAwaitStrategy = (StaticAwaitStrategy)strategy;

        assertThat(staticAwaitStrategy.getIp(), is("localhost"));
        assertThat(staticAwaitStrategy.getPorts().get(0), is(8080));
        assertThat(staticAwaitStrategy.getPorts().get(1), is(8089));
        assertThat(staticAwaitStrategy.getPollIterations(), is(3));
        assertThat(staticAwaitStrategy.getSleepPollTime(), is(200));
        assertThat(staticAwaitStrategy.getTimeUnit(), is(TimeUnit.SECONDS));
    }

    @Test
    public void should_create_native_await_strategy_if_no_strategy_is_provided() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_NO_STRATEGY);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
    }

    @Test
    public void should_create_polling_await_strategy() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_POLLING_STRATEGY);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_times() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_POLLING_STRATEGY_WITHOUT_DEFAULTS);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getPollIterations(), is(3));
        assertThat(((PollingAwaitStrategy)strategy).getSleepPollTime(), is(200));
    }

    @Test
    public void should_create_sleeping_await_strategy_with_specific_times() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_SLEEPING_STRATEGY_WITHOUT_DEFAULTS_AND_UNIT);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(SleepingAwaitStrategy.class));
        assertThat(((SleepingAwaitStrategy)strategy).getSleepTime(), is(200));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_times_and_unit() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_POLLING_STRATEGY_WITHOUT_DEFAULTS_AND_UNIT);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getPollIterations(), is(3));
        assertThat(((PollingAwaitStrategy)strategy).getSleepPollTime(), is(200));
        assertThat(((PollingAwaitStrategy)strategy).getTimeUnit(), is(TimeUnit.SECONDS));
        assertThat(((PollingAwaitStrategy)strategy).getType(), is("sscommand"));
    }

    @Test
    public void should_create_polling_await_strategy_with_specific_type() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_SSCOMMAND_STRATEGY);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(PollingAwaitStrategy.class));
        assertThat(((PollingAwaitStrategy)strategy).getType(), is("sscommand"));
    }

    @Test
    public void should_create_native_await_strategy() {

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) new Yaml().load(CONTENT_WITH_NATIVE_STRATEGY);
        @SuppressWarnings("unchecked")
        Map<String, Object> tomcatConfig = (Map<String, Object>) content.get("tomcat");

        AwaitStrategy strategy = AwaitStrategyFactory.create(null, cube, tomcatConfig);

        assertThat(strategy, instanceOf(NativeAwaitStrategy.class));
    }

}
