package com.guoshengkai.cloudflarefaker.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

public class WebSocketMessageSender {
    private final WebSocketSession session;
    private final Sinks.Many<WebSocketMessage> sink;

    protected Logger log = LoggerFactory.getLogger(WebSocketMessageSender.class);

    public WebSocketMessageSender(WebSocketSession session) {
        this.session = session;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void sendMessage(String payload) {
        WebSocketMessage message = session.textMessage(payload);
        synchronized (session) {
            Sinks.EmitResult result = sink.tryEmitNext(message);
            if (result.isFailure()) {
                log.error("Failed to send message: {}, {}", result, payload);
            }
        }
    }

    public void sendMessage(MessageBody messageBody) {
        sendMessage(JSON.toJSONString(messageBody));
    }

    // 显式指定返回类型为Publisher<WebSocketMessage>
    public Publisher<WebSocketMessage> getMessages() {
        return sink.asFlux();
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getSessionId() {
        return session.getId();
    }
}
