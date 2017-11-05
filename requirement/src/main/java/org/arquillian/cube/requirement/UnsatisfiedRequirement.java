package org.arquillian.cube.requirement;

import org.junit.Assume;
import org.junit.runners.model.Statement;

public class UnsatisfiedRequirement extends Statement {

    private final String message;

    UnsatisfiedRequirement(String message) {
        this.message = message;
    }

    @Override
    public void evaluate() {
        Assume.assumeTrue(message, false);
    }
}
