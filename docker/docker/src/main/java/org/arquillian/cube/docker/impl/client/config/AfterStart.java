package org.arquillian.cube.docker.impl.client.config;

public class AfterStart {

    private Copy copy;

    private CustomAfterStartAction customAfterStartAction;

    public AfterStart() {
    }

    public Copy getCopy() {
        return copy;
    }

    public void setCopy(Copy copy) {
        this.copy = copy;
    }

    public CustomAfterStartAction getCustomAfterStartAction() {
        return customAfterStartAction;
    }

    public void setCustomAfterStartAction(CustomAfterStartAction customAfterStartAction) {
        this.customAfterStartAction = customAfterStartAction;
    }
}
