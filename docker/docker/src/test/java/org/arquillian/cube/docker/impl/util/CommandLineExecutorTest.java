package org.arquillian.cube.docker.impl.util;

import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.spacelift.execution.ExecutionException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class CommandLineExecutorTest {

    @Test(expected = ExecutionException.class)
    public void shouldThrowAnIllegalArgumentExceptionWhenCommandNotFound() {
        CommandLineExecutor commandLineExecutor = new CommandLineExecutor();
        commandLineExecutor.execCommand("commandNotFound.sh");
    }
}
