package com.guoshengkai.cloudflarefaker.entitys;

import java.util.ArrayList;
import java.util.List;

public class ModelsResponse {
    private List<ModelInfo> models;
    private int count;
    private long lastUpdated;

    public ModelsResponse() {
        this.models = new ArrayList<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    public static class ModelInfo {
        private String name;
        private String id;
        private String status;
        private String provider;

        public ModelInfo(String name, String id, String status, String provider) {
            this.name = name;
            this.id = id;
            this.status = status;
            this.provider = provider;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }

    // Getters and setters
    public List<ModelInfo> getModels() { return models; }
    public void setModels(List<ModelInfo> models) {
        this.models = models;
        this.count = models.size();
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
