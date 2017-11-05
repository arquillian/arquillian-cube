package org.arquillian.cube.spi;

public class CubeControlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String cubeId;

    public CubeControlException(String cubeId, String message, Throwable cause) {
        super(message, cause);
        this.cubeId = cubeId;
    }

    public static CubeControlException failedCreate(String cubeId, Throwable cause) {
        return new CubeControlException(cubeId, "Could not create " + cubeId, cause);
    }

    public static CubeControlException failedStart(String cubeId, Throwable cause) {
        return new CubeControlException(cubeId, "Could not start " + cubeId, cause);
    }

    public static CubeControlException failedStop(String cubeId, Throwable cause) {
        return new CubeControlException(cubeId, "Could not stop " + cubeId, cause);
    }

    public static CubeControlException failedDestroy(String cubeId, Throwable cause) {
        return new CubeControlException(cubeId, "Could not destroy " + cubeId, cause);
    }

    public String getCubeId() {
        return cubeId;
    }
}
