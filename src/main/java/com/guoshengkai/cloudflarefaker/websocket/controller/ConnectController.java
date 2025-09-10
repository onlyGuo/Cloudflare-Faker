package com.guoshengkai.cloudflarefaker.websocket.controller;

import com.alibaba.fastjson2.JSONObject;
import com.guoshengkai.cloudflarefaker.websocket.MessageBody;
import com.guoshengkai.cloudflarefaker.websocket.WebSocketMessageSender;
import com.guoshengkai.cloudflarefaker.websocket.annotation.WebSocketController;
import com.guoshengkai.cloudflarefaker.websocket.annotation.WebSocketMapping;
import com.guoshengkai.cloudflarefaker.websocket.center.RegisteredSessionManager;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author gsk
 */
@WebSocketController
public class ConnectController {

    @WebSocketMapping("register")
    public MessageBody register(WebSocketMessageSender sender, JSONObject msg) {
        RegisteredSessionManager.registerSession(msg.getString("clientId"), sender.getSession());
        return MessageBody.create("register_ack")
                .append("status", "success")
                .append("data", Map.of(
                        "clientId", msg.getString("clientId")
                ));
    }
}
