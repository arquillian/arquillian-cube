package org.arquillian.cube.docker.impl.util;

import org.arquillian.spacelift.execution.ExecutionException;
import org.junit.Test;

public class CommandLineExecutorTest {

    @Test(expected = ExecutionException.class)
    public void shouldThrowAnIllegalArgumentExceptionWhenCommandNotFound() {
        CommandLineExecutor commandLineExecutor = new CommandLineExecutor();
        commandLineExecutor.execCommand("commandNotFound.sh");
    }

}
