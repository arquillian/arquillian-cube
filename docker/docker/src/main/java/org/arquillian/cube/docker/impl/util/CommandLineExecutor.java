package org.arquillian.cube.docker.impl.util;

import java.util.List;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.process.Command;
import org.arquillian.spacelift.process.CommandBuilder;
import org.arquillian.spacelift.process.ProcessResult;
import org.arquillian.spacelift.task.os.CommandTool;

public class CommandLineExecutor {

    public String execCommand(String... arguments) {
        return execCommandAsArray(arguments).get(0);
    }

    public List<String> execCommandAsArray(String... arguments) {
        Command allowExecCmd = new CommandBuilder(arguments).build();
        ProcessResult processResult = Spacelift.task(CommandTool.class)
            .command(allowExecCmd)
            .execute()
            .await();

        return processResult.output();
    }
}
