package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.BindingUtil;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class HttpAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "http";

    private static final String REGEXP_PREFIX = "regexp:";
    public static final String DOCKER_HOST = "dockerHost";
    private static final int DEFAULT_POLL_ITERATIONS = 10;

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private URL url;
    private int responseCode = 200;
    private Map<String, Object> headers;
    private String matcher;


    public HttpAwaitStrategy(final Cube<?> cube, final DockerClientExecutor dockerClientExecutor, final Await params, final boolean dind) {
        super(params.getSleepPollingTime());

        if (params.getIterations() != null) {
            this.pollIterations = params.getIterations();
        }

        if (params.getUrl() != null) {
            String url = params.getUrl();

            if (url.contains(DOCKER_HOST)) {
                url = url.replaceAll(DOCKER_HOST, (dind ? BindingUtil.getContainerIp(dockerClientExecutor, cube.getId()) : dockerClientExecutor.getDockerServerIp()));
            }

            try {
                this.url = new URL(url);

                Logger.getLogger(HttpAwaitStrategy.class.getName()).log(Level.INFO, "Http await strategy URL is: " + this.url.toExternalForm());

            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException("Http Await Strategy requires url field");
        }

        if (params.getResponseCode() != null) {
            this.responseCode = params.getResponseCode();
        }

        if (params.getHeaders() != null) {
            this.headers = params.getHeaders();
        }

        if (params.getMatch() != null) {
            this.matcher = params.getMatch();
        }
    }

    @Override
    public boolean await() {
        return Ping.ping(pollIterations, getSleepTime(), getTimeUnit(), () -> {

            HttpURLConnection urlConnection = null;

            try {

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                int connectionResponseCode = urlConnection.getResponseCode();
                if (responseCode != connectionResponseCode) {
                    return false;
                }

                if (matcher != null) {
                    String content = IOUtil.asString(urlConnection.getInputStream());

                    if (matcher.startsWith(REGEXP_PREFIX)) {
                        String pattern = matcher.substring(REGEXP_PREFIX.length());
                        final boolean matches = Pattern.compile(pattern, Pattern.DOTALL).matcher(content).matches();
                        if (!matches) return false;
                    } else {
                        final boolean matches = content.startsWith(matcher);
                        if (!matches) return false;
                    }
                }

                if (headers != null) {
                    final Set<String> keys = headers.keySet();

                    for (String key : keys) {
                        if (urlConnection.getHeaderField(key) != null) {
                            String connectionHeaderValue = urlConnection.getHeaderField(key);
                            if (!connectionHeaderValue.equals(headers.get(key))) {
                                return false;
                            }
                        } else {
                            // header has not set the required field yet
                            return false;
                        }
                    }
                }
            } catch (final Exception ignore) {
                return false;
            } finally {
                if (urlConnection != null) {
                    try {
                        urlConnection.disconnect();
                    } catch (final Exception ignore) {
                        //no-op
                    }
                }
            }

            return true;
        });
    }

    public String getUrl() {
        if (url == null) {
            return "";
        }
        return url.toString();
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getMatcher() {
        return matcher;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public int getPollIterations() {
        return pollIterations;
    }
}
