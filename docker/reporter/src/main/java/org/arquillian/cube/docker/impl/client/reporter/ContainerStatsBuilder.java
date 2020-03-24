package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.BlkioStatEntry;
import com.github.dockerjava.api.model.BlkioStatsConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContainerStatsBuilder {

    public static CubeStatistics updateStats(Statistics statistics) {

        CubeStatistics stats = new CubeStatistics();

        Map<String, Long> blkio = extractIORW(statistics.getBlkioStats());
        stats.setIoBytesRead(blkio.get("io_bytes_read"));
        stats.setIoBytesWrite(blkio.get("io_bytes_write"));

        MemoryStatsConfig memoryStats = statistics.getMemoryStats();
        stats.setMaxUsage(memoryStats.getMaxUsage());
        stats.setUsage(memoryStats.getUsage());
        stats.setLimit(memoryStats.getLimit());

        stats.setNetworks(extractNetworksStats(statistics.getNetworks()));

        return stats;
    }

    private static Map<String, Map<String, Long>> extractNetworksStats(Map<String, StatisticNetworksConfig> map) {
        Map<String, Map<String, Long>> nwStatsForEachNICAndTotal = new LinkedHashMap<>();
        if (map != null) {
            long totalRxBytes = 0, totalTxBytes = 0;

            for (Map.Entry<String, StatisticNetworksConfig> entry: map.entrySet()) {
                Map<String, Long> nwStats = new LinkedHashMap<>();
                String adapterName = entry.getKey();
                StatisticNetworksConfig adapter = entry.getValue();

                long rxBytes = adapter.getRxBytes();
                long txBytes = adapter.getTxBytes();

                nwStats.put("rx_bytes", rxBytes);
                nwStats.put("tx_bytes", txBytes);
                nwStatsForEachNICAndTotal.put(adapterName, nwStats);

                totalRxBytes += rxBytes;
                totalTxBytes += txBytes;
            }

            Map<String, Long> total = new LinkedHashMap<>();

            total.put("rx_bytes", totalRxBytes);
            total.put("tx_bytes", totalTxBytes);
            nwStatsForEachNICAndTotal.put("Total", total);
        }
        return nwStatsForEachNICAndTotal;
    }

    private static Map<String, Long> extractIORW(BlkioStatsConfig blkioStats) {
        Map<String, Long> blkrwStats = new LinkedHashMap<>();
        if (blkioStats != null) {
            List<BlkioStatEntry> bios = blkioStats.getIoServiceBytesRecursive();
            long read = 0, write = 0;
            if (bios != null) {
                for (BlkioStatEntry blkioStatEntry : bios) {
                    if (blkioStatEntry != null) {
                        switch (blkioStatEntry.getOp()) {
                            case "Read":
                                read =blkioStatEntry.getValue();
                                break;
                            case "Write":
                                write = blkioStatEntry.getValue();
                                break;
                        }
                    }
                }
            }
            blkrwStats.put("io_bytes_read", read);
            blkrwStats.put("io_bytes_write", write);
        }
        return blkrwStats;
    }
}
