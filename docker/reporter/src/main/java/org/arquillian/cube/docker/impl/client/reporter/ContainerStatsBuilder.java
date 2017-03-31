package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.Statistics;
import org.arquillian.cube.docker.impl.client.utils.NumberConversion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContainerStatsBuilder {

    public static CubeStatistics updateStats(Statistics statistics) {

        CubeStatistics stats = new CubeStatistics();

        Map<String, Long> blkio = extractIORW(statistics.getBlkioStats());
        Map<String, Long> memory = extractMemoryStats(statistics.getMemoryStats(), "usage", "max_usage", "limit");

        stats.setIoBytesRead(blkio.get("io_bytes_read"));
        stats.setIoBytesWrite(blkio.get("io_bytes_write"));
        stats.setMaxUsage(memory.get("max_usage"));
        stats.setUsage(memory.get("usage"));
        stats.setLimit(memory.get("limit"));

        stats.setNetworks(extractNetworksStats(statistics.getNetworks()));


        return stats;
    }

    private static Map<String, Map<String, Long>> extractNetworksStats(Map<String, Object> map) {
        Map<String, Map<String, Long>> nwStatsForEachNICAndTotal = new LinkedHashMap<>();
        if (map != null) {
            long totalRxBytes = 0, totalTxBytes = 0;

            for (Map.Entry<String, Object> entry: map.entrySet()) {
                Map<String, Long> nwStats = new LinkedHashMap<>();
                String adapterName = entry.getKey();
                if (entry.getValue() instanceof LinkedHashMap) {

                    Map<String, ?> adapter = (LinkedHashMap) entry.getValue();

                    long rxBytes = NumberConversion.convertToLong(adapter.get("rx_bytes"));
                    long txBytes = NumberConversion.convertToLong(adapter.get("tx_bytes"));

                    nwStats.put("rx_bytes", rxBytes);
                    nwStats.put("tx_bytes", txBytes);
                    nwStatsForEachNICAndTotal.put(adapterName, nwStats);

                    totalRxBytes += rxBytes;
                    totalTxBytes += txBytes;
                }
            }

            Map<String, Long> total = new LinkedHashMap<>();

            total.put("rx_bytes", totalRxBytes);
            total.put("tx_bytes", totalTxBytes);
            nwStatsForEachNICAndTotal.put("Total", total);
        }
        return nwStatsForEachNICAndTotal;
    }

    private static Map<String, Long> extractIORW(Map<String, Object> blkioStats) {
        Map<String, Long> blkrwStats = new LinkedHashMap<>();
        if (blkioStats != null && !blkioStats.isEmpty()) {
            List<LinkedHashMap> bios = (ArrayList<LinkedHashMap>) blkioStats.get("io_service_bytes_recursive");
            long read = 0, write = 0;
            if (bios != null) {
                for (Map<String, ?> io : bios) {
                    if (io != null) {
                        switch ((String) io.get("op")) {
                            case "Read":
                                read = NumberConversion.convertToLong(io.get("value"));
                                break;
                            case "Write":
                                write = NumberConversion.convertToLong(io.get("value"));
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

    private static Map<String, Long> extractMemoryStats(Map<String, Object> map, String... fields) {
        Map<String, Long> memory = new LinkedHashMap<>();
        if (map != null) {
            for (String field: fields) {
                long usage = NumberConversion.convertToLong(map.get(field));
                memory.put(field, usage);
            }
        }
        return memory;
    }
}
