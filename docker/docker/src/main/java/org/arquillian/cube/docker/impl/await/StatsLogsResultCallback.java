package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.async.ResultCallbackTemplate;


public class StatsLogsResultCallback extends ResultCallbackTemplate<StatsLogsResultCallback, Statistics> {

    private Statistics statistics = null;

    public StatsLogsResultCallback() {
    }

    @Override
    public void onNext(Statistics statistics) {
        if (statistics != null) {
            this.statistics = statistics;
            this.onComplete();
        }
    }

    public Statistics getStatistics() {
        return this.statistics;
    }

}

