package org.arquillian.cube.containerobject;

public class IsContainerObject {

    private Class<?> testClass;

    public IsContainerObject(Class<?> testClass) {
        this.testClass = testClass;
    }

    public Class<?> getTestClass() {
        return this.testClass;
    }
}
