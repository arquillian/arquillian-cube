package org.arquillian.cube.spi.metadata;

public interface CanExecuteProcessInContainer extends CubeMetadata {

    ExecResult exec(String...command);

    public static class ExecResult {
        private String output;
        private boolean isRunning;
        private int exitCode;

        public ExecResult(String output, boolean isRunning, int exitCode) {
            this.output = output;
            this.isRunning = isRunning;
            this.exitCode = exitCode;
        }

        public String getOutput() {
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
