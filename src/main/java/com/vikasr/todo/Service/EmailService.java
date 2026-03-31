package com.vikasr.todo.Service;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    boolean isMailDeliveryEnabled();

    CompletableFuture<Void> sendReminderEmail(String to, String subject,
                                              String title, String description, Long todoId, String token);

    CompletableFuture<Void> sendOverdueEmail(String to, String subject,
                                             String title, String description, Long todoId, String token);

    CompletableFuture<Void> sendEmail(String to, String subject, String body);
}
