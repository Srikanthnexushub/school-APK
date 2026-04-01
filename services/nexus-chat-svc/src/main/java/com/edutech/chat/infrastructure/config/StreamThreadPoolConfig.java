package com.edutech.chat.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class StreamThreadPoolConfig {

    @Bean("streamExecutor")
    public ExecutorService streamExecutor(
            @Value("${chat.stream-thread-pool-size:10}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize,
            r -> {
                Thread t = new Thread(r, "chat-stream-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });
    }
}
