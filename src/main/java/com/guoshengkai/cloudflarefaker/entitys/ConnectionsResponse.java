package com.guoshengkai.cloudflarefaker.entitys;

import java.util.ArrayList;
import java.util.List;

public class ConnectionsResponse {
    private int count;
    private long totalMessages;
    private List<ConnectionInfo> connections;

    public ConnectionsResponse() {
        this.connections = new ArrayList<>();
    }

    public static class ConnectionInfo {
        private String id;
        private String clientId;
        private long messages;
        private long connectedAt;
        private long lastPing;
        private boolean active;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public long getMessages() { return messages; }
        public void setMessages(long messages) { this.messages = messages; }

        public long getConnectedAt() { return connectedAt; }
        public void setConnectedAt(long connectedAt) { this.connectedAt = connectedAt; }

        public long getLastPing() { return lastPing; }
        public void setLastPing(long lastPing) { this.lastPing = lastPing; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    // Getters and setters
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }

    public List<ConnectionInfo> getConnections() { return connections; }
    public void setConnections(List<ConnectionInfo> connections) { this.connections = connections; }
}
