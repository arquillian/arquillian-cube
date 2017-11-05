package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;

public class LogScanningAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "log";

    private static final Logger logger = Logger.getLogger(LogScanningAwaitStrategy.class.getName());
    private static final String REGEXP_PREFIX = "regexp:";
    private final LogMatcher matcher;
    private int timeout = 15;
    private boolean stdOut;
    private boolean stdErr;
    private int occurrences = 1;
    private Cube<?> cube;
    private DockerClientExecutor dockerClientExecutor;

    public LogScanningAwaitStrategy(Cube<?> cube, DockerClientExecutor dockerClientExecutor, Await params) {
        super(params.getSleepPollingTime());

        this.cube = cube;
        this.dockerClientExecutor = dockerClientExecutor;

        this.stdOut = params.isStdOut();
        this.stdErr = params.isStdErr();

        this.occurrences = params.getOccurrences();

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

    public int getOccurrences() {
        return occurrences;
    }

    @Override
    public boolean await() {
        final DockerClient client = dockerClientExecutor.getDockerClient();
        final CountDownLatch containerUp = new CountDownLatch(1);

        try (final LogContainerResultCallback callback = new LogContainerResultCallback(containerUp, this.occurrences)) {

            client.logContainerCmd(cube.getId())
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .exec(callback);

            return containerUp.await(this.timeout, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, String.format("Log Await Strategy failed with %s", e.getMessage()));
            return false;
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

    private class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {

        private CountDownLatch containerUp;
        private int occurrences;

        public LogContainerResultCallback(CountDownLatch containerUp, int occurrences) {
            this.containerUp = containerUp;
            this.occurrences = occurrences;
        }

        @Override
        public void onNext(Frame item) {
            String line = new String(item.getPayload());

            if (matcher.match(line)) {
                this.occurrences--;
                if (this.occurrences == 0) {
                    this.containerUp.countDown();
                }
            }
        }
    }
}
