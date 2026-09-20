package com.himanshu.movieTicketBookingSystem.config;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    // Thread pool that delivers notifications off the request thread; if it is full the notification stays PENDING for the sweep.
    @Bean(name = Constants.Notifications.EXECUTOR)
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Constants.Notifications.CORE_POOL_SIZE);
        executor.setMaxPoolSize(Constants.Notifications.MAX_POOL_SIZE);
        executor.setQueueCapacity(Constants.Notifications.QUEUE_CAPACITY);
        executor.setThreadNamePrefix(Constants.Notifications.THREAD_PREFIX);
        executor.setRejectedExecutionHandler((r, e) -> log.warn("Notification queue full; the periodic sweep will deliver it"));
        return executor;
    }
}
