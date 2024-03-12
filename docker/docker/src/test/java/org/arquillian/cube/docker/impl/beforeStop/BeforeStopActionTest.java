package org.arquillian.cube.docker.impl.beforeStop;

import org.arquillian.cube.docker.impl.client.config.BeforeStop;
import org.arquillian.cube.docker.impl.client.config.CustomBeforeStopAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.DefaultCubeId;
import org.arquillian.cube.spi.beforeStop.BeforeStopAction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BeforeStopActionTest {

    private String containerId = "test";
    @Mock
    private DockerClientExecutor dockerClientExecutor;

    @Test
    public void should_be_able_to_create_custom_before_stop_actions() {
        BeforeStop beforeStop = new BeforeStop();
        CustomBeforeStopAction customBeforeStopAction = new CustomBeforeStopAction();
        customBeforeStopAction.setStrategy("org.arquillian.cube.docker.impl.beforeStop.CustomBeforeStopActionImpl");
        beforeStop.setCustomBeforeStopAction(customBeforeStopAction);

        BeforeStopAction beforeStopAction =
            BeforeStopActionFactory.create(dockerClientExecutor, new DefaultCubeId(containerId), customBeforeStopAction);

        assertThat(beforeStopAction, instanceOf(CustomBeforeStopActionInstantiator.class));
        CustomBeforeStopActionInstantiator customBeforeStopActionInstantiator =
            (CustomBeforeStopActionInstantiator) beforeStopAction;

        customBeforeStopActionInstantiator.doBeforeStop();
        verify(dockerClientExecutor, times(1)).getDockerUri();
    }
}
