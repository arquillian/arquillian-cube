package org.arquillian.cube;

import java.io.Serializable;

public class TopContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String[] titles;
    private String[][] processes;

    public TopContainer(String[] titles, String[][] processes) {
        this.titles = titles;
        this.processes = processes;
    }

    public String[] getTitles() {
        return this.titles;
    }

    public String[][] getProcesses() {
        return this.processes;
    }
}
