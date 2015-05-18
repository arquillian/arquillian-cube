package org.arquillian.cube.spi;

public enum ConnectionMode {
    STARTANDSTOP(false, true), STARTORCONNECT(true, true), STARTORCONNECTANDLEAVE(true, false);

    private boolean allowReconnect = false;
    private boolean stoppable = true;

    private ConnectionMode(boolean allowReconnect, boolean stoppable) {
        this.allowReconnect = allowReconnect;
        this.stoppable = stoppable;
    }

    public boolean isAllowReconnect() {
        return allowReconnect;
    }

    public boolean isStoppable() {
        return stoppable;
    }
}
