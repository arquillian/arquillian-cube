package org.arquillian.cube.docker.impl.client.config;

public class Image {
    private String name;
    private String tag;

    public Image(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public String toImageRef() {
        return toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
        Image other = (Image) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name + (tag != null ? ":" + tag : "");
    }

    public static Image valueOf(String image) {
        String name = null;
        String tag = null;

        String[] parts = image.split(":");
        name = parts[0];
        if (parts.length > 1) {
            tag = parts[1];
        }
        return new Image(name, tag);
    }
}
