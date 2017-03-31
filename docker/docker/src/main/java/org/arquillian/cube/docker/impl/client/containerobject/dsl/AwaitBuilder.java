package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.arquillian.cube.docker.impl.client.config.Await;

/**
 * Builder to create await objects. Polling comes by default if not set.
 */
public class AwaitBuilder {

    private static final int DEFAULT_TIMEOUT = 30;

    public static Await logAwait(String matching) {
        return logAwait(matching, DEFAULT_TIMEOUT);
    }

    public static Await logAwait(String matching, int timeoutInSeconds) {
        Await await = new Await();
        await.setStrategy("log");
        await.setMatch(matching);
        await.setTimeout(timeoutInSeconds);
        await.setStdOut(true);

        return await;
    }

    /**
     * @param serviceUrl
     *     You can use dockerHost reserved keyword, which is replaced at runtime for real docker host ip.
     */
    public static HttpAwaitBuilder httpAwait(URL serviceUrl, String messageContent) {
        return new HttpAwaitBuilder(serviceUrl, messageContent);
    }

    public static Await defaultHttpAwait(URL serviceUrl, String messageContent, int statusCode) {
        return httpAwait(serviceUrl, messageContent).withResponseCode(statusCode).build();
    }

    public static class HttpAwaitBuilder {

        private Await await;

        public HttpAwaitBuilder(URL url, String messageContent) {
            this.await = new Await();
            await.setStrategy("http");
            await.setMatch(messageContent);
            await.setUrl(url.toExternalForm());
            await.setSleepPollingTime(DEFAULT_TIMEOUT + " s");
            await.setIterations(10);
        }

        public HttpAwaitBuilder withResponseCode(int responseCode) {
            await.setResponseCode(responseCode);
            return this;
        }

        public HttpAwaitBuilder withSleepPollingTime(int time, TimeUnit unit) {
            final long l = unit.toMillis(time);
            await.setSleepPollingTime(l + "ms");

            return this;
        }

        public HttpAwaitBuilder withIterations(int iterations) {
            await.setIterations(iterations);
            return this;
        }

        public HttpAwaitBuilder withHeaders(Map<String, Object> headers) {
            await.setHeaders(headers);
            return this;
        }

        public HttpAwaitBuilder withHeaders(String key, Object value, Object... keyValues) {
            if (keyValues.length % 2 != 0) {
                throw new IllegalArgumentException("Key Values should be a pair of key, value");
            }

            final Map<String, Object> headers = new HashMap<>();
            headers.put(key, value);

            for (int i = 0; i < keyValues.length; i += 2) {
                headers.put((String) keyValues[i], keyValues[i + 1]);
            }

            return withHeaders(headers);
        }

        public Await build() {
            return this.await;
        }
    }
}
