package com.guoshengkai.cloudflarefaker.entitys;

public class ApiResponse {
    private Object data;
    private String message;
    private long timestamp;
    private String status;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Object data) {
        this();
        this.data = data;
        this.status = "success";
    }

    public ApiResponse(String message, Object data) {
        this(data);
        this.message = message;
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(data);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(message, data);
    }

    public static ApiResponse error(String message) {
        ApiResponse response = new ApiResponse();
        response.message = message;
        response.status = "error";
        return response;
    }

    // Getters and setters
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
