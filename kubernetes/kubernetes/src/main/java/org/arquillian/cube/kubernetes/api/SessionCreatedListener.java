package org.arquillian.cube.kubernetes.api;

public interface SessionCreatedListener {

    void start();

    void stop();

    void clean(String status);

    void display();
}
