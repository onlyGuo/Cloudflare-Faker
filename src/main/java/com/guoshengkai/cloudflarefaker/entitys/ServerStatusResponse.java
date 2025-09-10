package com.guoshengkai.cloudflarefaker.entitys;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Server status response structure.
 * @author gsk
 */
@Getter
@Setter
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
}
