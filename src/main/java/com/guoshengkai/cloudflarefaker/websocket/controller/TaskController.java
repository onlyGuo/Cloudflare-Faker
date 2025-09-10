package com.guoshengkai.cloudflarefaker.websocket.controller;

import com.alibaba.fastjson2.JSONObject;
import com.guoshengkai.cloudflarefaker.websocket.ResponseManager;
import com.guoshengkai.cloudflarefaker.websocket.annotation.WebSocketController;
import com.guoshengkai.cloudflarefaker.websocket.annotation.WebSocketMapping;

@WebSocketController
public class TaskController {

    @WebSocketMapping("task-response")
    public void taskResponse(JSONObject json){
        String taskId = json.getString("taskId");
        JSONObject data = json.getJSONObject("data");
        if ("stream".equals(data.getString("type"))){
            ResponseManager.completeResponseStream(taskId, data.get("data"));
        }else{
            ResponseManager.completeResponse(taskId, data.get("data"));
        }
    }

}
