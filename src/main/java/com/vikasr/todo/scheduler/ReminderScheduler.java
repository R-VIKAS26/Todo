package com.vikasr.todo.scheduler;

import com.vikasr.todo.Service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private static final int SCHEDULE_RATE_MS = 60000; // 1 minute

    @Value("${app.todo-overdue-hours:24}")
    private long overdueHours;
    private final EmailService emailService;
    private final JobLauncher jobLauncher;
    private final Job reminderEmailJob;

    @Scheduled(fixedRate = SCHEDULE_RATE_MS)
    public void sendReminder() {
        if (!emailService.isMailDeliveryEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        // Step-scoped readers depend on explicit job parameters for their cutoff values.
        JobParameters params = new JobParametersBuilder()
                .addString("cutoffTime", now.toString())
                .addString("overdueCutoffTime", now.minusHours(overdueHours).toString())
                .addLong("scheduledAt", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(reminderEmailJob, params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run reminder email batch job", e);
        }
    }
}
