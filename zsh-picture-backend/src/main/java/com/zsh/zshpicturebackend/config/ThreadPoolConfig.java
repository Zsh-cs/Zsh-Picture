package com.zsh.zshpicturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池
 */
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "cosClearExecutor")
    public ThreadPoolTaskExecutor cosClearExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(6);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量：当核心线程都在忙时，新线程会进入队列排队等候
        executor.setQueueCapacity(20);
        // 线程名称前缀，方便定位日志
        executor.setThreadNamePrefix("cos-clear-");
        // 拒绝策略：队列和最大线程都满了，交由主线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationMillis(60);
        // 初始化
        executor.initialize();
        return executor;
    }
}
