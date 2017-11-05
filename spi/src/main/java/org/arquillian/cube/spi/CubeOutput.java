package org.arquillian.cube.spi;

public class CubeOutput {

    private String standard;
    private String error;

    public CubeOutput(String standard, String error) {
        this.standard = standard;
        this.error = error;
    }

    public String getStandard() {
        return standard;
    }

    public String getError() {
        return error;
    }
}

