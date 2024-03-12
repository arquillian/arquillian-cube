package org.arquillian.cube.docker.impl.afterStart;

import org.arquillian.cube.docker.impl.client.config.AfterStart;
import org.arquillian.cube.docker.impl.client.config.CustomAfterStartAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.DefaultCubeId;
import org.arquillian.cube.spi.afterStart.AfterStartAction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AfterStartActionTest {

    private String containerId = "test";

    @Mock
    private DockerClientExecutor dockerClientExecutor;

    @Test
    public void should_be_able_to_create_custom_after_start_actions() {
        AfterStart afterStart = new AfterStart();
        CustomAfterStartAction customAfterStartAction = new CustomAfterStartAction();
        customAfterStartAction.setStrategy("org.arquillian.cube.docker.impl.afterStart.CustomAfterStartActionImpl");
        afterStart.setCustomAfterStartAction(customAfterStartAction);

        AfterStartAction afterStartAction =
            AfterStartActionFactory.create(dockerClientExecutor, new DefaultCubeId(containerId), customAfterStartAction);

        assertThat(afterStartAction, instanceOf(CustomAfterStartActionInstantiator.class));
        CustomAfterStartActionInstantiator customAfterStartActionInstantiator =
            (CustomAfterStartActionInstantiator) afterStartAction;

        customAfterStartActionInstantiator.doAfterStart();
        verify(dockerClientExecutor, times(1)).getDockerUri();
    }
}
