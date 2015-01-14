package org.arquillian.cube.impl.containerless;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DaytimeServer {

    public static void main(String[] args) {

        Undertow server = Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send(simpleDateFormat.format(new Date()) + System.lineSeparator());
                    }
                }).build();
        server.start();
    }
}
