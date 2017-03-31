package com.github.dockerjava;


public class CubeOutput {

    private String output;
    private String error;

    public CubeOutput(String output, String error) {
        this.output = output;
        this.error = error;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
