package org.arquillian.cube.kubernetes.impl.utils;

import java.util.List;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.process.Command;
import org.arquillian.spacelift.process.CommandBuilder;
import org.arquillian.spacelift.process.ProcessResult;
import org.arquillian.spacelift.task.os.CommandTool;

public class CommandExecutor {

    public List<String> execCommand(String command) {
        if (command.isEmpty()) {
            throw new IllegalStateException("command to run can't be empty");
        }
        final String[] arguments = command.split("\\s+");
        return execCommandAsArray(arguments);
    }

    public List<String> execCommand(String... arguments) {
        return execCommandAsArray(arguments);
    }

    private List<String> execCommandAsArray(String... arguments) {
        Command allowExecCmd = new CommandBuilder(arguments).build();
        ProcessResult processResult = Spacelift.task(CommandTool.class)
            .command(allowExecCmd)
            .execute()
            .await();

        return processResult.output();
    }
}
