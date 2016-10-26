package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LogScanningAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "log";

    private static final String REGEXP_PREFIX = "regexp:";
    private static final int DEFAULT_TIME_OUT = 15;

    private int timeout = DEFAULT_TIME_OUT;

    private boolean stdOut;
    private boolean stdErr;

    private Cube<?> cube;

    private DockerClientExecutor dockerClientExecutor;

    private final LogMatcher matcher;

    public LogScanningAwaitStrategy(Cube<?> cube, DockerClientExecutor dockerClientExecutor, Await params) {
        super(params.getSleepPollingTime());

        this.cube = cube;
        this.dockerClientExecutor = dockerClientExecutor;

        this.stdOut = params.isStdOut();
        this.stdErr = params.isStdErr();

        if (params.getMatch().startsWith(REGEXP_PREFIX)) {
            matcher = new RegexpLogMatcher(params.getMatch().substring(REGEXP_PREFIX.length()));
        } else {
            matcher = new ContainsLogMatcher(params.getMatch());
        }

        if (params.getTimeout() != null) {
            this.timeout = params.getTimeout();
        }

    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isStdOut() {
        return stdOut;
    }

    public boolean isStdErr() {
        return stdErr;
    }

    @Override
    public boolean await() {
        final DockerClient client = dockerClientExecutor.getDockerClient();
        final CountDownLatch containerUp = new CountDownLatch(1);

        try (final LogContainerResultCallback callback = new LogContainerResultCallback(containerUp)) {
            try {

                client.logContainerCmd(cube.getId())
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .exec(callback);

                boolean result = containerUp.await(this.timeout, TimeUnit.SECONDS);
                return result;
            } catch (InterruptedException e) {
                return false;
            }
        } catch (IOException e) {

            return false;
        }

    }

    private class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {

        private CountDownLatch containerUp;

        public LogContainerResultCallback(CountDownLatch containerUp) {
            this.containerUp = containerUp;
        }

        @Override
        public void onNext(Frame item) {
            String line = new String(item.getPayload());

            if (matcher.match(line)) {
                this.containerUp.countDown();
            }
        }
    }

    private interface LogMatcher {

        boolean match(String line);

    }

    private static final class ContainsLogMatcher implements LogMatcher {

        private String substring;

        public ContainsLogMatcher(String substring) {
            this.substring = substring;
        }

        @Override
        public boolean match(String line) {
            return line.contains(substring);
        }

    }

    private static final class RegexpLogMatcher implements LogMatcher {

        private Pattern regex;

        public RegexpLogMatcher(String pattern) {
            regex = Pattern.compile(pattern, Pattern.DOTALL);
        }

        @Override
        public boolean match(String line) {
            return regex.matcher(line).matches();
        }

    }

}
