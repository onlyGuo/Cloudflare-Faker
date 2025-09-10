package com.guoshengkai.cloudflarefaker.controller;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class TestController {


    @GetMapping("/test")
    public Object test() {
        return Map.of("code", 200, "msg", "success");
    }

    @GetMapping("/test2")
    public Flux<ServerSentEvent<String>> test2() {
        Flux<ServerSentEvent<String>> dataFlux = Flux.create(emitter -> {
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(5000);
                        emitter.next(ServerSentEvent.<String>builder()
                                .data("data-" + (i + 1))
                                .build());
                    }
                    emitter.complete();
                } catch (InterruptedException e) {
                    emitter.error(e);
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);

        Flux<ServerSentEvent<String>> heartbeatFlux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> ServerSentEvent.<String>builder()
                        .data("heartbeat")
                        .build());

        return Flux.merge(dataFlux, heartbeatFlux)
                .takeUntilOther(dataFlux.ignoreElements().then());
    }

}
