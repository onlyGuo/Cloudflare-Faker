package com.guoshengkai.cloudflarefaker.entitys;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Server status response structure.
 * @author gsk
 */
public class ServerStatusResponse {
    private String status;
    private String version;
    private int port;
    private long startTime;
    private long uptimeMs;
    private String uptimeFormatted;
    private int activeConnections;
    private long totalMessages;
    private Map<String, Long> memory;
    private long timestamp;

    public ServerStatusResponse() {
        this.memory = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getUptimeMs() {
        return uptimeMs;
    }

    public void setUptimeMs(long uptimeMs) {
        this.uptimeMs = uptimeMs;
    }

    public String getUptimeFormatted() {
        return uptimeFormatted;
    }

    public void setUptimeFormatted(String uptimeFormatted) {
        this.uptimeFormatted = uptimeFormatted;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Map<String, Long> getMemory() {
        return memory;
    }

    public void setMemory(Map<String, Long> memory) {
        this.memory = memory;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
