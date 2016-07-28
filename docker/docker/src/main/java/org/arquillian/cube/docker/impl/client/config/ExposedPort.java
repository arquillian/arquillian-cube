package org.arquillian.cube.docker.impl.client.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExposedPort {
    private int exposed;
    private String type = "tcp";

    public ExposedPort(int exposed, String type) {
        this.exposed = exposed;
        if(type != null) {
            this.type = type;
        }
    }

    public int getExposed() {
        return exposed;
    }

    public void setExposed(int exposed) {
        this.exposed = exposed;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return exposed + "/" + type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + exposed;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExposedPort other = (ExposedPort) obj;
        if (exposed != other.exposed)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    public static ExposedPort valueOf(String exp) {
        int exposed;
        String type = null;
        String[] parts = exp.split("/");
        exposed = Integer.parseInt(parts[0].trim());
        if (parts.length > 1) {
            type = parts[1].trim();
        }
        return new ExposedPort(exposed, type);
    }

    public static Collection<ExposedPort> valuesOf(Collection<String> ports) {
        if (ports == null) {
            return null;
        }
        List<ExposedPort> result = new ArrayList<ExposedPort>();
        for (String port : ports) {
            result.add(valueOf(port));
        }
        return result;
    }
}
