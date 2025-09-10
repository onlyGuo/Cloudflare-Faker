package com.guoshengkai.cloudflarefaker.websocket;

import java.util.LinkedHashMap;

/**
 * Simple message body structure for WebSocket communication.
 * @author gsk
 */
public class MessageBody extends LinkedHashMap<String, Object> {
    public static MessageBody create(String type) {
        MessageBody messageBody = new MessageBody();
        messageBody.put("type", type);
        return messageBody;
    }

    public MessageBody append(String key, Object value) {
        this.put(key, value);
        return this;
    }
}
