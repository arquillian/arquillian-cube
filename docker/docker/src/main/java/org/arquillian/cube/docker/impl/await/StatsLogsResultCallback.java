package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.async.ResultCallbackTemplate;

import java.util.concurrent.CountDownLatch;

public class StatsLogsResultCallback extends ResultCallbackTemplate<StatsLogsResultCallback, Statistics> {

    private Statistics statistics;
    private CountDownLatch countDownLatch;

    public StatsLogsResultCallback(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onNext(Statistics statistics) {
        if (statistics != null) {
            this.statistics = statistics;
            this.onComplete();
        }
        this.countDownLatch.countDown();
    }

    public Statistics getStatistics() {
        return this.statistics;
    }

}

