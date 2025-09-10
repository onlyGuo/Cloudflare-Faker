package com.guoshengkai.cloudflarefaker.websocket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ResponseManager {

    private static Map<String, TaskResponse> responseMap = new ConcurrentHashMap<>();


    /**
     * 获取任务响应
     * @param taskId 任务ID
     * @param i 超时时间，单位毫秒
     * @return 响应对象
     */
    public static Object getTaskResponse(String taskId, int i) {
        TaskResponse taskResponse = responseMap.get(taskId);
        if (taskResponse == null) {
            taskResponse = new TaskResponse();
            taskResponse.setTimeout(i);
            taskResponse.setCompleted(false);
            responseMap.put(taskId, taskResponse);
        }
        long startTime = System.currentTimeMillis();
        long timeout = taskResponse.getTimeout();
        while (!taskResponse.isCompleted()) {
            if (System.currentTimeMillis() - startTime > timeout) {
                removeResponse(taskId);
                throw new RuntimeException("Response timeout");
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Object response = taskResponse.getResponse();
        removeResponse(taskId);
        return response;
    }

    public static void getTaskResponseStream(String taskId, int startTimeout, int completedTimeout, Consumer<String> callback) {
        TaskResponse taskResponse = responseMap.get(taskId);
        if (taskResponse == null) {
            taskResponse = new TaskResponse();
            taskResponse.setTimeout(completedTimeout);
            taskResponse.setStartTimeout(startTimeout);
            taskResponse.setCompleted(false);
            responseMap.put(taskId, taskResponse);
        }
        long startTime = System.currentTimeMillis();
        long startTimeoutTime = taskResponse.getStartTimeout();
        long timeout = taskResponse.getTimeout();
        boolean started = false;
        while (!taskResponse.isCompleted()) {
            if (!started && System.currentTimeMillis() - startTime > startTimeoutTime) {
                removeResponse(taskId);
                throw new RuntimeException("Response start timeout");
            }
            if (started && System.currentTimeMillis() - taskResponse.startTime > timeout) {
                removeResponse(taskId);
                throw new RuntimeException("Response completed timeout");
            }
            List<Object> responseStream = taskResponse.getResponseStream();
            while (responseStream != null && !responseStream.isEmpty()) {
                synchronized (responseStream) {
                    if (responseStream.isEmpty()){
                        continue;
                    }
                    Object response = responseStream.removeFirst();
                    if (response != null) {
                        started = true;
                        callback.accept(response.toString());
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        removeResponse(taskId);
    }

    @Getter
    @Setter
    public static class TaskResponse {
        private WebSocketSession session;
        private Object response;
        /**
         * 开始接收Response的时间
         */
        private long startTime;
        private long timeout;
        private long startTimeout;
        private boolean completed;
        private List<Object> responseStream = new java.util.LinkedList<>();
    }

    public static void completeResponse(String taskId, Object response) {
        TaskResponse taskResponse = responseMap.get(taskId);
        if (taskResponse != null) {
            taskResponse.setResponse(response);
            taskResponse.setCompleted(true);
        }
    }

    public static void completeResponseStream(String taskId, Object response) {
        TaskResponse taskResponse = responseMap.get(taskId);
        if (taskResponse != null) {
            List<Object> responseStream = taskResponse.getResponseStream();
            if (responseStream != null) {
                synchronized (responseStream) {
                    if ("[!!DONE!!]".equals(response.toString())) {
                        taskResponse.setCompleted(true);
                    }else{
                        if (taskResponse.startTime == 0) {
                            taskResponse.startTime = System.currentTimeMillis();
                        }
                        responseStream.add(response);
                    }
                }
            }
        }
    }

    private static void removeResponse(String taskId) {
        responseMap.remove(taskId);
    }
}
