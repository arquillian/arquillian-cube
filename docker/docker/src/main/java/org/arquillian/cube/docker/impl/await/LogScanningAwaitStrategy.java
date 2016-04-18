package org.arquillian.cube.docker.impl.await;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.docker.impl.util.PingCommand;
import org.arquillian.cube.spi.Cube;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.async.ResultCallbackTemplate;

public class LogScanningAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "log";
        
    private static final String REGEXP_PREFIX = "regexp:";
    
    private static final int DEFAULT_POLL_ITERATIONS = 10;
    
    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    
    private boolean stdOut;
    private boolean stdErr;
    
    private Cube<?> cube;
    
    private DockerClientExecutor dockerClientExecutor;
    
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    private final LogMatcher matcher; 
    
    public LogScanningAwaitStrategy(Cube<?> cube, DockerClientExecutor dockerClientExecutor, Await params) {
        super(params.getSleepPollingTime());
        
        this.cube = cube;
        this.dockerClientExecutor = dockerClientExecutor;
        
        if (params.getIterations() != null) {
            this.pollIterations = params.getIterations();
        }
        
        this.stdOut = params.isStdOut();
        this.stdErr = params.isStdErr();

        if (params.getMatch().startsWith(REGEXP_PREFIX)) {
            matcher = new RegexpLogMatcher(params.getMatch().substring(REGEXP_PREFIX.length()));
        } else {
            matcher = new ContainsLogMatcher(params.getMatch());
        }
    }
    
    public int getPollIterations() {
        return pollIterations;
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
        
        Info info = client.infoCmd().exec();
        
        int since = parseTimestamp(info.getSystemTime());
        
        final LogContainerResultCallback callback = new LogContainerResultCallback(since);
        
        return Ping.ping(pollIterations, getSleepTime(), getTimeUnit(), new PingCommand() {
            @Override
            public boolean call() {
                try {
                    client.logContainerCmd(cube.getId())
                            .withStdOut(stdOut).withStdErr(stdErr)
                            .withTimestamps(true).withSince(callback.getLastTimestamp())
                            .exec(callback).awaitCompletion();
                }
                catch (InterruptedException e) {
                    // do nothing
                }
                
                return callback.isFound();
            }
        });
    }
    
    private int parseTimestamp(String s) {
        try {
            return (int) dateFormat.parse(s.substring(0, s.indexOf('.'))).getTime() / 1000;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Timestamp parse failure: " + s, e);
        }
    }
    
    private class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {
        
        private int lastTimestamp;
        
        private boolean found;
        
        public LogContainerResultCallback(int since) {
            this.lastTimestamp = since;
        }
        
        public int getLastTimestamp() {
            return lastTimestamp;
        }
        
        public boolean isFound() {
            return found;
        }

        @Override
        public void onNext(Frame item) {
            String line = new String(item.getPayload());
            lastTimestamp = parseTimestamp(line);
            
            if (!found) {
                found = matcher.match(line);
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
