package org.arquillian.cube;

/**
 * Simple class to represent the Cube ID of the current active Cube.
 * <br/><br/>
 * Usage:
 * <br/><br/>
 * <p>
 * <pre><code>
 * &#64;ArquillianResource
 * private CubeID cubeId;
 * </code></pre>
 *
 * @author aslak
 */
public class CubeID {

    private String cubeId;

    public CubeID(String cubeId) {
        if (cubeId == null) {
            throw new IllegalArgumentException("cubeId must be provided");
        }
        this.cubeId = cubeId;
    }

    public String get() {
        return cubeId;
    }

    @Override
    public String toString() {
        return cubeId;
    }
}
