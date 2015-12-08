package org.arquillian.cube.docker.impl.util;


import org.junit.Test;

public class CommandLineExecutorTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnIllegalArgumentExceptionWhenCommandNotFound() {
        CommandLineExecutor commandLineExecutor = new CommandLineExecutor();
        commandLineExecutor.execCommand("commandNotFound.sh");
    }

}
