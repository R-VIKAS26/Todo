package com.vikasr.todo.Service.impl;

import com.vikasr.todo.Service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.todo-base-url:http://localhost:8080}")
    private String todoBaseUrl;

    @Value("${app.todo-deep-link-pattern:http://localhost:4200/todos/%d}")
    private String todoDeepLinkPattern;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String mailFrom;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public boolean isMailDeliveryEnabled() {
        return mailEnabled || hasSmtpCredentials();
    }

    @Override
    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> sendReminderEmail(String to, String subject, String title, String description,Long todoId, String token) {
        sendTodoEmail(to, subject, title, description, todoId, token, "templates/reminder-email.html");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> sendOverdueEmail(String to, String subject, String title, String description, Long todoId, String token) {
        sendTodoEmail(to, subject, title, description, todoId, token, "templates/overdue-email.html");
        return CompletableFuture.completedFuture(null);
    }

    private void sendTodoEmail(String to,
                               String subject,
                               String title,
                               String description,
                               Long todoId,
                               String token,
                               String templatePath) {
        if (!isMailDeliveryEnabled()) {
            log.info("Mail delivery disabled; skipping email for todo {}", todoId);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(to);
            helper.setSubject(subject);
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }

            // Load HTML template
            ClassPathResource resource =
                    new ClassPathResource(templatePath);

            String html = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // Replace placeholders
            html = html.replace("{{title}}", title);
            html = html.replace("{{description}}", description);
            String appLink = buildTodoDeepLink(todoId);
            String completionLink = buildCompletionLink(todoId, token);
            html = html.replace("{{link}}", appLink);
            html = html.replace("{{appLink}}", appLink);
            html = html.replace("{{completeLink}}", completionLink);
            helper.setText(html, true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to send todo email", e);
        }
    }

    private boolean hasSmtpCredentials() {
        return mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    private String buildTodoDeepLink(Long todoId) {
        String route = String.format(todoDeepLinkPattern, todoId);
        if (route.startsWith("http://") || route.startsWith("https://")) {
            return route;
        }
        String baseUrl = trimTrailingSlash(todoBaseUrl);
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        return baseUrl + route;
    }

    private String buildCompletionLink(Long todoId, String token) {
        return trimTrailingSlash(todoBaseUrl)
                + "/api/v1/todos/complete/"
                + todoId
                + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    @Override
    public CompletableFuture<Void> sendEmail(String to, String subject, String body) {
        return CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                
                helper.setFrom("noreply@todoapp.com");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, true);
                
                mailSender.send(message);
                log.info("Email sent successfully to {}", to);
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", to, e.getMessage());
            }
        });
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
