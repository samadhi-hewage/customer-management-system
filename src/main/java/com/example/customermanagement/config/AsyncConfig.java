package com.example.customermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// -------------------------------------------------------
// This class enables @Async so BulkUploadService can run
// file processing in a background thread without blocking
// the HTTP response.
// -------------------------------------------------------
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "bulkUploadExecutor")
    public Executor bulkUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);     // always-on threads
        executor.setMaxPoolSize(5);      // max threads under load
        executor.setQueueCapacity(10);   // jobs waiting if all threads busy
        executor.setThreadNamePrefix("bulk-upload-");
        executor.initialize();
        return executor;
    }
}