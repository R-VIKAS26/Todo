package com.vikasr.todo;

import com.vikasr.todo.Service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.mail.enabled=true",
        "spring.task.scheduling.enabled=false",
        "management.health.mail.enabled=false"
})
class EmailServiceAsyncIntegrationTests {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void reminderEmailRunsOnVirtualThreadExecutor() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        AtomicBoolean ranOnVirtualThread = new AtomicBoolean(false);
        AtomicReference<String> sendThreadName = new AtomicReference<>();
        String testThreadName = Thread.currentThread().getName();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doAnswer(invocation -> {
            Thread thread = Thread.currentThread();
            ranOnVirtualThread.set(thread.isVirtual());
            sendThreadName.set(thread.getName());
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        CompletableFuture<Void> result = emailService.sendReminderEmail(
                "user@example.com",
                "Todo Reminder",
                "Write tests",
                "Verify async delivery",
                1L,
                "token-123"
        );

        result.get(5, TimeUnit.SECONDS);

        assertTrue(ranOnVirtualThread.get(), "Mail send should run on a virtual thread");
        assertTrue(sendThreadName.get().startsWith("todo-vt-"), "Mail send should use the virtual-thread executor");
        assertNotEquals(testThreadName, sendThreadName.get(), "Mail send should run off the calling thread");
    }
}
