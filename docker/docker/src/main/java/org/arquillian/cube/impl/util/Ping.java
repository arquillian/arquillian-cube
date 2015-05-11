package org.arquillian.cube.impl.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.impl.docker.DockerClientExecutor;

public final class Ping {

    private Ping() {
        super();
    }

    public static boolean ping(DockerClientExecutor dockerClientExecutor, String containerId, String command, int totalIterations, long sleep, TimeUnit timeUnit) {
        boolean result = false;
        int iteration = 0;

        do {
            result = execContainerPing(dockerClientExecutor, containerId, command);
            if(!result) {
                iteration++;
                try {
                    timeUnit.sleep(sleep);
                } catch (InterruptedException e) {
                }
            }
        } while(!result && iteration < totalIterations);

        return result;
    }

    public static boolean ping(String host, int port, int totalIterations, long sleep, TimeUnit timeUnit) {
        boolean result = false;
        int iteration = 0;

        do {
            result = ping(host, port);
            if(!result) {
                iteration++;
                try {
                    timeUnit.sleep(sleep);
                } catch (InterruptedException e) {
                }
            }
        } while(!result && iteration < totalIterations);

        return result;
    }


    private static boolean execContainerPing(DockerClientExecutor dockerClientExecutor, String containerId, String command) {
        String result = dockerClientExecutor.execStart(containerId, new String[]{"sh", "-c", command});
        try {
            int numberOfListenConnectons = Integer.parseInt(result.trim());
            //This number is based in that a port will be opened only as tcp or as udp.
            //We will need another issue to modify cube internals to save if port is udp or tcp.
            return numberOfListenConnectons > 0;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    private static boolean ping(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            return true;
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null) try { socket.close(); } catch(IOException e) {}
        }
    }
}
