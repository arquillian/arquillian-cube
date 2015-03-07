package org.arquillian.cube.impl.client.container.remote.command;

public class CopyFileDirectoryCommand extends AbstractCubeCommand<String> {

    private static final long serialVersionUID = 1L;

    private String from;
    private String to;

    public CopyFileDirectoryCommand(String cubeId, String from, String to) {
        super(cubeId);
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
