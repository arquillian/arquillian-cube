package org.arquillian.cube.docker.impl.client.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Link {

    private String name;
    private String alias;

    public Link(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAliasSet() {
        return alias != null;
    }

    public String getAlias() {
        if (alias == null) {
            return name;
        }
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (alias != null) {
            sb.append(":").append(alias);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((alias == null) ? 0 : alias.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Link other = (Link) obj;
        if (alias == null) {
            if (other.alias != null)
                return false;
        } else if (!alias.equals(other.alias))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public static Link valueOf(String links) {
        String[] link = links.split(":");
        String name = link[0];
        String alias = null;
        if (link.length == 2) {
            alias = link[1];
        }
        return new Link(name, alias);
    }

    public static Collection<Link> valuesOf(Collection<String> links) {
        if (links == null) {
            return null;
        }
        List<Link> result = new ArrayList<Link>();
        for (String link : links) {
            result.add(valueOf(link));
        }
        return result;
    }
}
