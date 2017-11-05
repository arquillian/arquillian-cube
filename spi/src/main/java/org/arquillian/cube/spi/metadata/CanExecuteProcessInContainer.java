package org.arquillian.cube.spi.metadata;

import org.arquillian.cube.spi.CubeOutput;

public interface CanExecuteProcessInContainer extends CubeMetadata {

    ExecResult exec(String... command);

    public static class ExecResult {
        private CubeOutput output;
        private boolean isRunning;
        private int exitCode;

        public ExecResult(CubeOutput output, boolean isRunning, int exitCode) {
            this.output = output;
            this.isRunning = isRunning;
            this.exitCode = exitCode;
        }

        public CubeOutput getOutput() {
            return output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isRunning() {
            return isRunning;
        }
    }
}
