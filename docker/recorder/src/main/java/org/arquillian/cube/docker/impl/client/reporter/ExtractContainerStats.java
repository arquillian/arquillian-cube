package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.Statistics;
import org.arquillian.cube.docker.impl.client.utils.NumberConversion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtractContainerStats {

    public static Map<String, Map<String, ?>> getStats(Statistics statistics, Boolean decimal) {

        Map<String, Map<String, ?>> stats = new LinkedHashMap<>();
        if (statistics != null){
            Map<String, Map<String, String>> networks = extractNetworksStats(statistics.getNetworks(), decimal);
            Map<String, String> memory = extractMemoryStats(statistics.getMemoryStats(), decimal, "usage", "max_usage", "limit");
            Map<String, String> blkio = extractIORW(statistics.getBlkioStats(), decimal);
            stats.put("network", networks);
            stats.put("memory", memory);
            stats.put("block I/O", blkio);
        }
        return stats;
    }

    private static Map<String, Map<String, String>> extractNetworksStats(Map<String, Object> map, boolean decimal) {
        Map<String, Map<String, String>> nwStatsForEachNICAndTotal = new LinkedHashMap<>();
        if (map != null) {
            long totalRxBytes = 0, totalTxBytes = 0;

            for (Map.Entry<String, Object> entry: map.entrySet()) {
                Map<String, String> nwStats = new LinkedHashMap<>();
                String adapterName = entry.getKey();
                if (entry.getValue() instanceof LinkedHashMap) {

                    Map<String, ?> adapter = (LinkedHashMap) entry.getValue();

                    long rxBytes = NumberConversion.convertToLong(adapter.get("rx_bytes"));
                    long txBytes = NumberConversion.convertToLong(adapter.get("tx_bytes"));

                    nwStats.put("rx_bytes", NumberConversion.humanReadableByteCount(rxBytes, decimal));
                    nwStats.put("tx_bytes", NumberConversion.humanReadableByteCount(txBytes, decimal));
                    nwStatsForEachNICAndTotal.put(adapterName, nwStats);

                    totalRxBytes += rxBytes;
                    totalTxBytes += txBytes;
                }
            }

            Map<String, String> total = new LinkedHashMap<>();

            total.put("rx_bytes", NumberConversion.humanReadableByteCount(totalRxBytes, decimal));
            total.put("tx_bytes", NumberConversion.humanReadableByteCount(totalTxBytes, decimal));
            nwStatsForEachNICAndTotal.put("Total", total);
        }
        return nwStatsForEachNICAndTotal;
    }

    private static Map<String, String> extractIORW(Map<String, Object> blkioStats, Boolean decimal) {
        Map<String, String> blkrwStats = new LinkedHashMap<>();
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
            blkrwStats.put("I/O Bytes Read", NumberConversion.humanReadableByteCount(read, decimal));
            blkrwStats.put("I/O Bytes Write", NumberConversion.humanReadableByteCount(write, decimal));
        }
        return blkrwStats;
    }

    private static Map<String, String> extractMemoryStats(Map<String, Object> map, boolean decimal, String... fields) {
        Map<String, String> memory = new LinkedHashMap<>();
        if (map != null) {
            for (String field: fields) {
                long usage = NumberConversion.convertToLong(map.get(field));
                memory.put(field, NumberConversion.humanReadableByteCount(usage, decimal));
            }
        }
        return memory;
    }
}
