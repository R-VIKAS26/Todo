package com.vikasr.todo.batch;

import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Repository.TodoRepo;
import com.vikasr.todo.Service.EmailService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Configuration
@RequiredArgsConstructor
public class ReminderBatchConfig {

    private static final int CHUNK_SIZE = 10;

    // Query constants
    private static final String REMINDER_QUERY = """
                select t
                        from Todo t
                        where t.reminderTime <= :cutoffTime
                          and t.completed = false
                          and (t.reminderSent = false or t.reminderSent is null)
                          and (t.archived = false or t.archived is null)
                        order by t.id
                        """;
    
    private static final String OVERDUE_QUERY = """
                select t
                        from Todo t
                        where coalesce(t.dueTime, t.reminderTime) <= :overdueCutoffTime
                          and t.completed = false
                          and (t.archived = false or t.archived is null)
                        order by t.id
                        """;

    private final EntityManagerFactory entityManagerFactory;
    private final EmailService emailService;
    private final TodoRepo todoRepo;

    @Bean
    public Job reminderEmailJob(JobRepository jobRepository,
                                Step reminderEmailStep,
                                Step overdueEmailStep) {
        return new JobBuilder("reminderEmailJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(reminderEmailStep)
                .next(overdueEmailStep)
                .build();
    }

    @Bean
    public Step reminderEmailStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  JpaPagingItemReader<Todo> reminderTodoReader,
                                  ItemWriter<Todo> reminderTodoWriter) {
        return new StepBuilder("reminderEmailStep", jobRepository)
                .<Todo, Todo>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(reminderTodoReader)
                .writer(reminderTodoWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Todo> reminderTodoReader(
            @Value("#{jobParameters['cutoffTime']}") String cutoffTime) {
        LocalDateTime cutoff = LocalDateTime.parse(cutoffTime);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cutoffTime", cutoff);

        return new JpaPagingItemReaderBuilder<Todo>()
                .name("reminderTodoReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("""
                        select t
                        from Todo t
                        where t.reminderTime <= :cutoffTime
                          and t.completed = false
                          and (t.reminderSent = false or t.reminderSent is null)
                          and (t.archived = false or t.archived is null)
                        order by t.id
                        """)
                .parameterValues(parameters)
                .build();
    }

    @Bean
    public ItemWriter<Todo> reminderTodoWriter() {
        return items -> {
            List<Todo> todos = new ArrayList<>(items.getItems());
            List<CompletableFuture<Void>> sendOperations = new ArrayList<>();
            for (Todo todo : todos) {
                if (todo.getToken() == null || todo.getToken().isBlank()) {
                    todo.setToken(UUID.randomUUID().toString());
                }
                sendOperations.add(emailService.sendReminderEmail(
                        todo.getEmail(),
                        "Todo Reminder",
                        todo.getTitle(),
                        todo.getDescription(),
                        todo.getId(),
                        todo.getToken()
                ));
            }
            CompletableFuture.allOf(sendOperations.toArray(CompletableFuture[]::new)).join();
            for (Todo todo : todos) {
                todo.setReminderSent(true);
            }
            todoRepo.saveAll(todos);
        };
    }

    @Bean
    public Step overdueEmailStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 JpaPagingItemReader<Todo> overdueTodoReader,
                                 ItemWriter<Todo> overdueTodoWriter) {
        return new StepBuilder("overdueEmailStep", jobRepository)
                .<Todo, Todo>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(overdueTodoReader)
                .writer(overdueTodoWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Todo> overdueTodoReader(
            @Value("#{jobParameters['overdueCutoffTime']}") String overdueCutoffTime) {
        LocalDateTime overdueCutoff = LocalDateTime.parse(overdueCutoffTime);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("overdueCutoffTime", overdueCutoff);

        return new JpaPagingItemReaderBuilder<Todo>()
                .name("overdueTodoReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("""
                        select t
                        from Todo t
                        where coalesce(t.dueTime, t.reminderTime) <= :overdueCutoffTime
                          and t.completed = false
                          and (t.overdueNotificationSent = false or t.overdueNotificationSent is null)
                          and (t.archived = false or t.archived is null)
                        order by t.id
                        """)
                .parameterValues(parameters)
                .build();
    }

    @Bean
    public ItemWriter<Todo> overdueTodoWriter() {
        return items -> {
            List<Todo> todos = new ArrayList<>(items.getItems());
            List<CompletableFuture<Void>> sendOperations = new ArrayList<>();
            for (Todo todo : todos) {
                if (todo.getToken() == null || todo.getToken().isBlank()) {
                    todo.setToken(UUID.randomUUID().toString());
                }
                sendOperations.add(emailService.sendOverdueEmail(
                        todo.getEmail(),
                        "Todo Overdue",
                        todo.getTitle(),
                        todo.getDescription(),
                        todo.getId(),
                        todo.getToken()
                ));
            }
            CompletableFuture.allOf(sendOperations.toArray(CompletableFuture[]::new)).join();
            for (Todo todo : todos) {
                todo.setOverdueNotificationSent(true);
            }
            todoRepo.saveAll(todos);
        };
    }
}
