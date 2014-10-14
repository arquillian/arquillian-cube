package org.arquillian.cube.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Ping {

    private Ping() {
        super();
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
