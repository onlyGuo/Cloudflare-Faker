package com.guoshengkai.cloudflarefaker.websocket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 客户端信息类
 * 用于存储和管理客户端的连接信息
 * @author gsk
 */
public class ClientInfo {

    private String clientId;

    private String ipAddress;

    private int port;

    private int messageCount;

    private long connectedAt;

    private long lastPing;

    /**
     * 是否空闲
     */
    private boolean active;

    private WebSocketSession session;

    public WebSocketMessageSender getMessageSender() {
        return (WebSocketMessageSender) session.getAttributes()
                .putIfAbsent("__message_sender", new WebSocketMessageSender(session));
    }

    /**
     * 接收响应
     * @param taskId 任务ID
     * @param i 超时时间，单位毫秒
     * @return 响应对象
     */
    public Object receiveResponse(String taskId, int i) {
        return ResponseManager.getTaskResponse(taskId, i);
    }

    /**
     * 接收响应流
     * @param taskId 任务ID
     * @param startTimeout 开始超时时间，单位毫秒
     * @param completedTimeout 完成超时时间，单位毫秒
     * @param callback 回调函数
     */
    public void receiveResponseStream(String taskId, int startTimeout, int completedTimeout, Consumer<String> callback) {
        ResponseManager.getTaskResponseStream(taskId, startTimeout, completedTimeout, callback);
    }

    /**
     * 接收响应流，开始和完成超时时间相同
     * @param taskId 任务ID
     * @param timeout 超时时间，单位毫秒
     * @param callback 回调函数
     */
    public void receiveResponseStream(String taskId, int timeout, Consumer<String> callback) {
        ResponseManager.getTaskResponseStream(taskId, timeout, timeout, callback);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(long connectedAt) {
        this.connectedAt = connectedAt;
    }

    public long getLastPing() {
        return lastPing;
    }

    public void setLastPing(long lastPing) {
        this.lastPing = lastPing;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }
}
