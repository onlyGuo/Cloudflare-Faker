package com.guoshengkai.cloudflarefaker.controller;

import com.guoshengkai.cloudflarefaker.entitys.FetchCommand;
import com.guoshengkai.cloudflarefaker.entitys.JavascriptCommand;
import com.guoshengkai.cloudflarefaker.websocket.MessageBody;
import com.guoshengkai.cloudflarefaker.websocket.center.RegisteredSessionManager;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("api")
public class ApiController {

    /**
     * 执行远程Fetch命令，返回完整结果
     * @param command Fetch命令
     * @return 执行结果
     */
    @PostMapping("remote-fetch")
    public Object executeFetch(@RequestBody FetchCommand command){
        String taskId = "task-" + UUID.randomUUID().toString().replace("-", "");
        command.setStream(false);
        return RegisteredSessionManager.getClientAndExecute(client -> {
            // 发送给Websocket
            client.getMessageSender().sendMessage(MessageBody
                    .create("fetch-command").append("taskId", taskId).append("data", command));
            return client.receiveResponse(taskId, 60000);
        });
    }

    /**
     * 执行远程Fetch命令，返回SSE流
     * @param command Fetch命令
     * @return SSE流
     */
    @PostMapping("remote-fetch-stream")
    public Flux<ServerSentEvent<String>> executeFetchStream(@RequestBody FetchCommand command){
        String taskId = "task-" + UUID.randomUUID().toString().replace("-", "");
        command.setStream(true);
        System.out.println("Starting SSE stream for taskId: " + taskId);
        Flux<ServerSentEvent<String>> dataFlux = Flux.create(emitter -> {
            Thread.startVirtualThread(() -> {
                RegisteredSessionManager.getClientAndExecute(client -> {
                    // 发送给Websocket
                    client.getMessageSender().sendMessage(MessageBody
                            .create("fetch-command").append("taskId", taskId).append("data", command));
                    client.receiveResponseStream(taskId, 60000, 120000, chunk -> {
                        System.out.println("Received chunk: " + chunk);
                        emitter.next(ServerSentEvent.<String>builder()
                                .data(chunk)
                                .build());

                    });
                    return null;
                });
                emitter.next(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build());
                emitter.complete();
            });
        }, FluxSink.OverflowStrategy.BUFFER);

        Flux<ServerSentEvent<String>> heartbeatFlux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> ServerSentEvent.<String>builder()
                        .data("")
                        .build());
        // 发送[DONE]后结束, 或者120秒无数据后结束
        return Flux.merge(dataFlux, heartbeatFlux)
                .takeUntil(event -> "[DONE]".equals(event.data()))
                .doOnComplete(() -> System.out.println("SSE stream completed at taskId: " + taskId))
                .doOnCancel(() -> System.out.println("SSE stream cancelled at taskId: " + taskId));
    }

    /**
     * 执行远程Javascript脚本
     * @param command 脚本命令
     * @return 执行结果
     */
    @PostMapping("remote-script")
    public Object executeScript(@RequestBody JavascriptCommand command) {
        String taskId = "task-js-" + UUID.randomUUID().toString().replace("-", "");
        return RegisteredSessionManager.getClientAndExecute(client -> {
            // 发送给Websocket
            client.getMessageSender().sendMessage(MessageBody
                    .create("execute-script")
                    .append("taskId", taskId).append("data", command));
            return client.receiveResponse(taskId, 60000);
        });
    }

    /**
     * 执行远程Javascript脚本
     * @param command 脚本命令
     * @return 执行结果
     */
    @PostMapping("remote-html")
    public Object remoteHtml(@RequestBody JavascriptCommand command) {
        String taskId = "task-js-" + UUID.randomUUID().toString().replace("-", "");
        command.setType("LOAD_HTML");
        return RegisteredSessionManager.getClientAndExecute(client -> {
            // 发送给Websocket
            client.getMessageSender().sendMessage(MessageBody
                    .create("execute-script").append("taskId", taskId).append("data", command));
            return client.receiveResponse(taskId, 60000);
        });
    }
}
