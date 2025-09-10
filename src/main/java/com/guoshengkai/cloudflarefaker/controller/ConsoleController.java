package com.guoshengkai.cloudflarefaker.controller;

import com.guoshengkai.cloudflarefaker.entitys.*;
import com.guoshengkai.cloudflarefaker.websocket.ClientInfo;
import com.guoshengkai.cloudflarefaker.websocket.center.RegisteredSessionManager;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 控制台接口
 * 提供系统状态、连接信息等API
 * @author gsk
 */
@RestController
@RequestMapping("console")
public class ConsoleController {

    private final Date startTime = new Date();

    @Resource
    private Environment environment;

    @Resource
    private ApiController apiController;

    @GetMapping(value = "test")
    public ApiResponse test() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello from API");
        data.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(data);
    }

    @GetMapping(value = "/health")
    public ApiResponse health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("server", "Netty");
        return ApiResponse.success(data);
    }

    @GetMapping(value = "/status")
    public ServerStatusResponse getServerStatus() {
        Runtime runtime = Runtime.getRuntime();

        long uptimeMs = System.currentTimeMillis() - startTime.getTime();
        long uptimeSeconds = uptimeMs / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;

        ServerStatusResponse response = new ServerStatusResponse();
        response.setStatus("success");
        response.setVersion("1.0-SNAPSHOT");
        response.setPort(environment.getProperty("server.port", Integer.class, 8080));
        response.setStartTime(startTime.getTime());
        response.setUptimeMs(uptimeMs);
        response.setUptimeFormatted(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        response.setActiveConnections(RegisteredSessionManager.getRegisteredSessionCount());
        response.setTotalMessages(RegisteredSessionManager.getTotalMessages());

        Map<String, Long> memoryMap = new HashMap<>();
        memoryMap.put("total", runtime.totalMemory());
        memoryMap.put("max", runtime.maxMemory());
        memoryMap.put("free", runtime.freeMemory());
        memoryMap.put("used", memoryMap.get("total") - memoryMap.get("free"));
        response.setMemory(memoryMap);
        return response;
    }

    @PostMapping(value = "/echo")
    public ApiResponse echo(Object body) {
        Map<String, Object> data = new HashMap<>();
        data.put("echo", body);
        data.put("received_at", System.currentTimeMillis());
        return ApiResponse.success(data);
    }

    @GetMapping(value = "/connections")
    public ConnectionsResponse getConnections() {
        List<ClientInfo> clients = RegisteredSessionManager.getAllRegisteredSessions();

        ConnectionsResponse response = new ConnectionsResponse();
        response.setCount(clients.size());
        response.setTotalMessages(RegisteredSessionManager.getTotalMessages());

        for (ClientInfo client : clients) {
            ConnectionsResponse.ConnectionInfo connectionInfo = new ConnectionsResponse.ConnectionInfo();
            connectionInfo.setId(client.getSession().getId());
            connectionInfo.setClientId(client.getClientId());
            connectionInfo.setMessages(client.getMessageCount());
            connectionInfo.setConnectedAt(client.getConnectedAt());
            connectionInfo.setLastPing(client.getLastPing());
            connectionInfo.setActive(client.isActive());
            response.getConnections().add(connectionInfo);
        }

        return response;
    }

    @GetMapping(value = "/models")
    public ModelsResponse getModels() {
        ModelsResponse response = new ModelsResponse();
        List<ModelsResponse.ModelInfo> models = Arrays.asList(
                new ModelsResponse.ModelInfo("GPT-4", "gpt-4-0125-preview", "active", "OpenAI"),
                new ModelsResponse.ModelInfo("Claude-3 Opus", "claude-3-opus-20240229", "active", "Anthropic"),
                new ModelsResponse.ModelInfo("Claude-3 Sonnet", "claude-3-sonnet-20240229", "active", "Anthropic"),
                new ModelsResponse.ModelInfo("Gemini Pro", "gemini-pro-1.5", "active", "Google"),
                new ModelsResponse.ModelInfo("LLaMA 2 70B", "llama-2-70b-chat", "maintenance", "Meta"),
                new ModelsResponse.ModelInfo("Mistral Large", "mistral-large-latest", "active", "Mistral AI"),
                new ModelsResponse.ModelInfo("PaLM 2", "palm-2-chat-bison", "deprecated", "Google")
        );

        response.setModels(models);
        return response;
    }

}
