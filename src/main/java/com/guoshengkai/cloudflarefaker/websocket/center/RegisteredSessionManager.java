package com.guoshengkai.cloudflarefaker.websocket.center;

import com.guoshengkai.cloudflarefaker.websocket.ClientInfo;
import com.guoshengkai.cloudflarefaker.websocket.WebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

public class RegisteredSessionManager {

    protected static Logger log = LoggerFactory.getLogger(WebSocketMessageSender.class);

    private static Map<String, ClientInfo> registeredSessions = new ConcurrentHashMap<>();

    private static int totalMessages = 0;

    public static int incrementAndGetTotalMessages() {
        return ++totalMessages;
    }

    public static int getTotalMessages() {
        return totalMessages;
    }

    public static int getRegisteredSessionCount() {
        return registeredSessions.size();
    }

    public static List<ClientInfo> getAllRegisteredSessions() {
        return new ArrayList<>(registeredSessions.values().stream().toList());
    }

    public static void registerSession(String clientId, WebSocketSession session) {
        log.info("Registered session: {}", clientId);
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setClientId(clientId);
        clientInfo.setSession(session);
        clientInfo.setConnectedAt(System.currentTimeMillis());
        clientInfo.setActive(true);
        clientInfo.setLastPing(System.currentTimeMillis());
        if (registeredSessions.putIfAbsent(session.getId(), clientInfo) != null) {
            log.warn("Session with clientId {} already registered, replacing.", clientId);
            registeredSessions.replace(session.getId(), clientInfo);
        }else{
            registeredSessions.put(session.getId(), clientInfo);
            log.info("Session with clientId {} registered successfully.", clientId);
        }
    }

    public static void unregisterSession(WebSocketSession session) {
        unregisterSession(session.getId());
    }

    public static void unregisterSession(String sessionId) {
        ClientInfo remove = registeredSessions.remove(sessionId);
        if (null != remove){
            try {
                if (remove.getSession().isOpen()){
                    remove.getSession().close().subscribe();
                }
            } catch (Exception e) {
                log.error("Error closing session: {}", sessionId, e);
            }finally {
                log.info("Unregistered session: {}", remove.getClientId());
            }
        }
    }

    /**
     * Get all active connections
     * @return
     *      ClientInfo list
     */
    public static List<ClientInfo> getActiveConnections(){
        return new ArrayList<>(registeredSessions.values().stream()
                .filter(ClientInfo::isActive).toList());
    }

    public static<T> T getClientAndExecute(Function<ClientInfo, T> callback) {
        List<ClientInfo> clientInfoList = getActiveConnections();
        long startTime = System.currentTimeMillis();
        while (clientInfoList.isEmpty()){
            if (System.currentTimeMillis() - startTime > 10000){
                throw new RuntimeException("No active clients available after waiting for 10 seconds.");
            }
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            clientInfoList = getActiveConnections();
        }
        // 随机选择一个客户端
        int randomIndex = (int) (Math.random() * clientInfoList.size());
        ClientInfo clientInfo = clientInfoList.get(randomIndex);
        return callback.apply(clientInfo);
    }
}
