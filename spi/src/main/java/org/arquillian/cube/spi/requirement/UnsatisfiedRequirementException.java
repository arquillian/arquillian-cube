package org.arquillian.cube.spi.requirement;

public class UnsatisfiedRequirementException extends Exception {

    public UnsatisfiedRequirementException(String message) {
        super(message);
    }
}
