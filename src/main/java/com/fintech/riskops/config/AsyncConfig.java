package com.fintech.riskops.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool configuration for async bulk operations.
 *
 * Design: Bulk freeze/unfreeze of 10,000 merchants could block an API
 * thread for minutes. Offloading to a dedicated thread pool means:
 * - API returns immediately with a job ID
 * - Client polls for status
 * - Thread pool is bounded (won't exhaust resources)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.bulk-operations.core-pool-size:4}")
    private int corePoolSize;

    @Value("${async.bulk-operations.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${async.bulk-operations.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "bulkOperationsExecutor")
    public Executor bulkOperationsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("bulk-ops-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
