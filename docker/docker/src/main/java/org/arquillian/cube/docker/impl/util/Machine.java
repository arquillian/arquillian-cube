package org.arquillian.cube.docker.impl.util;

public class Machine {

    private static final int NAME_INDEX = 0;
    private static final int ACTIVE_INDEX = 1;
    private static final int DRIVER_INDEX = 2;
    private static final int STATE_INDEX = 3;
    private static final int URL_INDEX = 4;
    private static final int SWARM_INDEX = 5;

    private String name;
    private String active;
    private String driver;
    private String state;
    private String url;
    private String swarm;

    private Machine(String name, String active, String driver, String state, String url, String swarm) {
        super();

        this.name = name;
        this.active = active;
        this.driver = driver;
        this.state = state;
        this.url = url;
        this.swarm = swarm;
    }

    public static final Machine toMachine(String[] result) {
        String swarm = "";
        if (result.length >= SWARM_INDEX + 1) {
            swarm = result[SWARM_INDEX];
        }

        return new Machine(result[NAME_INDEX], result[ACTIVE_INDEX], result[DRIVER_INDEX], result[STATE_INDEX], result[URL_INDEX], swarm);
    }

    public String getName() {
        return name;
    }

    public String getActive() {
        return active;
    }

    public String getDriver() {
        return driver;
    }

    public String getState() {
        return state;
    }

    public String getUrl() {
        return url;
    }

    public String getSwarm() {
        return swarm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Machine machine = (Machine) o;

        if (name != null ? !name.equals(machine.name) : machine.name != null) return false;
        return !(url != null ? !url.equals(machine.url) : machine.url != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
