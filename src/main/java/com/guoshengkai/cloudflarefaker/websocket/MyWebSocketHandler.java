package com.guoshengkai.cloudflarefaker.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.guoshengkai.cloudflarefaker.websocket.annotation.WebSocketController;
import com.guoshengkai.cloudflarefaker.websocket.annotation.WebSocketMapping;
import com.guoshengkai.cloudflarefaker.websocket.center.RegisteredSessionManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MyWebSocketHandler implements WebSocketHandler {

    @Resource
    private ApplicationContext applicationContext;

    private Map<String, WebSocketInvoker> webSocketControllers = new HashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        WebSocketMessageSender messageSender =  new WebSocketMessageSender(session);
        session.getAttributes().put("__message_sender", messageSender);

        // 先建立发送流，确保在处理接收消息前就已经有订阅者
        Mono<Void> output = session.send(messageSender.getMessages());

        Mono<Void> close = session.closeStatus()
                .doOnNext(status -> handleDisconnect(session.getId())).then();

        // 接收消息并处理
        WebSocketMessageSender finalMessageSender = messageSender;
        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(payload -> {
                    // 在这里处理接收到的消息
                    if (payload.startsWith("{") && payload.endsWith("}")) {
                        log.debug("Received message: {}", payload);
                        JSONObject json = JSON.parseObject(payload);
                        if (json.containsKey("type")) {
                            String type = json.getString("type");
                            if ("ping".equals(type)) {
                                finalMessageSender.sendMessage("{\"type\":\"pong\"}");
                            } else {
                                RegisteredSessionManager.incrementAndGetTotalMessages();
                                WebSocketInvoker webSocketInvoker = webSocketControllers.get(type);
                                if (webSocketInvoker == null) {
                                    throw new IllegalStateException("No WebSocket handler found for type: " + type);
                                }
                                webSocketInvoker.invoke(finalMessageSender, json);
                            }
                        } else {
                            log.warn("Message does not contain 'type' field: {}", payload);
                        }
                    } else {
                        // 处理其他类型的消息
                        log.debug("收到无效的消息格式: {}", payload);
                    }
                })
                .then();

        // 关键：确保两个流都被处理
        return Mono.zip(input, output, close)
                .doFinally(signal -> handleDisconnect(session.getId())).then();
    }

    /**
     * 处理断开连接事件
     */
    private synchronized void handleDisconnect(String sessionId) {
        RegisteredSessionManager.unregisterSession(sessionId);
    }

    @PostConstruct
    protected void initWebSocketControllers() {
        // 获取所有标注了 @WebSocketController 注解的类
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(WebSocketController.class);
        for (Object controller : controllers.values()) {
            log.info("Found WebSocketController: {}", controller.getClass().getName());
            // 遍历类中的方法，查找标注了 @WebSocketMapping 注解的方法
            for (Method method : controller.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(WebSocketMapping.class)) {
                    WebSocketMapping mapping = method.getAnnotation(WebSocketMapping.class);
                    if (null == mapping) {
                        continue;
                    }
                    String path = mapping.value();
                    if (null == path || path.isEmpty()) {
                        log.warn("WebSocketMapping value is empty in {}.{}",
                                controller.getClass().getName(), method.getName());
                        continue;
                    }
                    if (webSocketControllers.containsKey(path)) {
                        throw new IllegalStateException("Duplicate WebSocketMapping for path: %s in %s.%s"
                                .formatted(path, controller.getClass().getName(), method.getName()));
                    }
                    webSocketControllers.put(path, new WebSocketInvoker(controller, method));
                    log.info("Registered WebSocket route: {} -> {}.{}", path,
                            controller.getClass().getName(), method.getName());
                }
            }
        }

    }

    @Getter
    private class WebSocketInvoker {
        private final Object handler;
        private final Method method;

        private WebSocketInvoker(Object handler, Method method) {
            this.handler = handler;
            this.method = method;
        }

        public void invoke(WebSocketMessageSender sender, JSONObject json) {
            try {
                Class<?>[] parameterTypes = method.getParameterTypes();
                List<Object> parameters = new java.util.ArrayList<>();
                for (Class<?> paramType : parameterTypes) {
                    if (paramType.equals(WebSocketSession.class)) {
                        parameters.add(sender.getSession());
                    } else if (paramType.equals(JSONObject.class)) {
                        parameters.add(json);
                    }else if (paramType.equals(String.class)) {
                        parameters.add(json.toJSONString());
                    } else if (paramType.equals(WebSocketMessageSender.class)) {
                        parameters.add(sender);
                    } else {
                        throw new IllegalStateException("Unsupported parameter type: " + paramType.getName());
                    }
                }
                Object invoke = method.invoke(handler, parameters.toArray());
                if (null == invoke) {
                    return;
                }
                if (invoke instanceof String) {
                    sender.sendMessage((String) invoke);
                } else if (invoke instanceof Mono) {
                    ((Mono<?>) invoke).subscribe(result -> {
                        if (result instanceof String) {
                            sender.sendMessage((String) result);
                        } else {
                            log.warn("WebSocket handler returned unsupported Mono result type: {}",
                                    result.getClass().getName());
                        }
                    }, error -> log.error("Error in WebSocket handler Mono: ", error));
                } else {
                    sender.sendMessage(JSON.toJSONString(invoke));
                }
            } catch (Exception e) {
                String msg = "Error invoking WebSocket handler method: %s.%s"
                        .formatted(handler.getClass().getName(), method.getName());
                throw new RuntimeException(msg, e);
            }
        }
    }
}
