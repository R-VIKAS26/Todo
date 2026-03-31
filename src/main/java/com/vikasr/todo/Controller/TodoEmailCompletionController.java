package com.vikasr.todo.Controller;

import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Repository.TodoRepo;
import com.vikasr.todo.Service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
public class TodoEmailCompletionController {

    private static final DateTimeFormatter EMAIL_PAGE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final TodoRepo todoRepo;
    private final TodoService todoService;

    @Value("${app.todo-base-url:http://localhost:8080}")
    private String todoBaseUrl;

    @Value("${app.todo-deep-link-pattern:http://localhost:4200/todos/%d}")
    private String todoDeepLinkPattern;

    @GetMapping(value = "/complete/{id}", produces = "text/html")
    @Operation(summary = "Complete todo from email link", description = "Completes a todo using the secure email token. Returns HTML.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTML response"),
            @ApiResponse(responseCode = "400", description = "Invalid token")
    })
    public ModelAndView completeFromEmail(@PathVariable Long id,
                                          @Parameter(description = "Secure token from reminder email")
                                          @RequestParam String token,
                                          Model model) {
        Todo todo = todoRepo.findById(id).orElseThrow();

        if (!token.equals(todo.getToken())) {
            populatePage(model,
                    "Link Invalid",
                    "This completion link is invalid or has expired.",
                    "Request a fresh reminder email and try again.",
                    "#ffedd5",
                    "#f97316",
                    "Link issue",
                    id,
                    null,
                    null);
            return new ModelAndView("completion-result", model.asMap());
        }

        if (todo.isCompleted()) {
            populatePage(model,
                    "Already Completed",
                    "This task was already marked complete.",
                    "No further action is needed.",
                    "#fef3c7",
                    "#d97706",
                    "Already done",
                    id,
                    todo.getTitle(),
                    todo.getDueTime() != null ? todo.getDueTime() : todo.getReminderTime());
            return new ModelAndView("completion-result", model.asMap());
        }

        todoService.markComplete(id);
        populatePage(model,
                "Task Completed",
                "Nice work. The task has been marked complete successfully.",
                "Your progress has been saved in Todo App.",
                "#dcfce7",
                "#16a34a",
                "Completed successfully",
                id,
                todo.getTitle(),
                todo.getDueTime() != null ? todo.getDueTime() : todo.getReminderTime());
        return new ModelAndView("completion-result", model.asMap());
    }

    private void populatePage(Model model,
                              String title,
                              String headline,
                              String supportingText,
                              String accentSoft,
                              String accentStrong,
                              String statusLabel,
                              Long todoId,
                              String taskTitle,
                              LocalDateTime scheduleTime) {
        String resolvedTaskTitle = taskTitle != null ? taskTitle : "Task";
        String scheduleLabel = scheduleTime != null
                ? EMAIL_PAGE_DATE_FORMAT.format(scheduleTime)
                : "No schedule set";

        model.addAttribute("pageTitle", title);
        model.addAttribute("headline", headline);
        model.addAttribute("supportingText", supportingText);
        model.addAttribute("accentSoft", accentSoft);
        model.addAttribute("accentStrong", accentStrong);
        model.addAttribute("statusLabel", statusLabel);
        model.addAttribute("taskTitle", resolvedTaskTitle);
        model.addAttribute("scheduleLabel", scheduleLabel);
        model.addAttribute("appLink", buildTodoDeepLink(todoId));
        model.addAttribute("shareBody", URLEncoder.encode(headline + " - " + resolvedTaskTitle, StandardCharsets.UTF_8));
        model.addAttribute("footerText", supportingText);
        model.addAttribute("successState", "Completed successfully".equals(statusLabel));
        model.addAttribute("statusIcon", resolveStatusIcon(statusLabel));
        model.addAttribute("brandName", "Todo App");
        model.addAttribute("brandTagline", "Focused work, clear outcomes.");
    }

    private String buildTodoDeepLink(Long todoId) {
        String baseUrl = todoBaseUrl != null ? todoBaseUrl.replaceAll("/+$", "") : "";
        String route = String.format(todoDeepLinkPattern, todoId);
        if (route.startsWith("http://") || route.startsWith("https://")) {
            return route;
        }
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        return baseUrl + route;
    }

    private String resolveStatusIcon(String statusLabel) {
        return switch (statusLabel) {
            case "Completed successfully" -> "✓";
            case "Already done" -> "•";
            default -> "!";
        };
    }
}
