package org.arquillian.cube.spi.requirement;

public interface Requirement<T> {

    /**
     * Check if the specified requirement is met in a given context.
     *
     * @param context
     *     the target context.
     *
     * @throws UnsatisfiedRequirementException
     *     when the requirement is not satisfied.
     */
    void check(T context) throws UnsatisfiedRequirementException;
}
