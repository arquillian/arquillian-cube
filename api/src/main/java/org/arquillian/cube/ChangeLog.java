package org.arquillian.cube;

import java.io.Serializable;

public class ChangeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String path;
    private int kind;

    public ChangeLog(String path, int kind) {
        this.path = path;
        this.kind = kind;
    }

    public String getPath() {
        return this.path;
    }

    public int getKind() {
        return this.kind;
    }
}
