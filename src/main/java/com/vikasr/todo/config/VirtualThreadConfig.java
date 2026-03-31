package com.vikasr.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@Configuration
public class VirtualThreadConfig {

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("todo-vt-");
        executor.setVirtualThreads(true);
        executor.setTaskTerminationTimeout(5000);
        executor.setCancelRemainingTasksOnClose(true);
        return executor;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setThreadNamePrefix("todo-scheduler-");
        scheduler.setVirtualThreads(true);
        scheduler.setTaskTerminationTimeout(5000);
        scheduler.setCancelRemainingTasksOnClose(true);
        return scheduler;
    }
}
