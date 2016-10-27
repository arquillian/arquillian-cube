package org.arquillian.cube.docker.impl.client.reporter;

import java.util.Map;

public class CubeStatistics {

    private Long rxBytes;
    private Long txBytes;
    private Long ioBytesRead;
    private Long ioBytesWrite;
    private Long usage;
    private Long maxUsage;
    private Long limit;
    private Map<String, Map<String, Long>> networks;

    public Long getIoBytesRead() {
        return ioBytesRead;
    }

    public void setIoBytesRead(Long ioBytesRead) {
        this.ioBytesRead = ioBytesRead;
    }

    public Long getIoBytesWrite() {
        return ioBytesWrite;
    }

    public void setIoBytesWrite(Long ioBytesWrite) {
        this.ioBytesWrite = ioBytesWrite;
    }

    public Long getRxBytes() {
        return rxBytes;
    }

    public void setRxBytes(Long rxBytes) {
        this.rxBytes = rxBytes;
    }

    public Long getTxBytes() {
        return txBytes;
    }

    public void setTxBytes(Long txBytes) {
        this.txBytes = txBytes;
    }

    public Long getUsage() {
        return usage;
    }

    public void setUsage(Long usage) {
        this.usage = usage;
    }

    public Long getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(Long maxUsage) {
        this.maxUsage = maxUsage;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public Map<String, Map<String, Long>> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Map<String, Long>> networks) {
        this.networks = networks;
    }
}
