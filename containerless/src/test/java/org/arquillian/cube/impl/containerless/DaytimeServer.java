package org.arquillian.cube.impl.containerless;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DaytimeServer {

    public static void main(String[] args) {

        for (;;) {
            try (ServerSocket serverSocket = new ServerSocket(8080);
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(
                            clientSocket.getOutputStream(), true);) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
                out.write(simpleDateFormat.format(new Date())+System.lineSeparator());
                out.flush();
            } catch (IOException e) {
                System.out
                        .println("Exception caught when trying to listen on port "
                                + 8080 + " or listening for a connection");
                System.out.println(e.getMessage());
            }
        }
    }

}
