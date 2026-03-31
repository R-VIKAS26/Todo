package com.vikasr.todo;

import com.vikasr.todo.DTO.TodoAnalyticsResponseDTO;
import com.vikasr.todo.DTO.TodoRequestDTO;
import com.vikasr.todo.DTO.TodoResponseDTO;
import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Model.RecurrencePattern;
import com.vikasr.todo.Repository.TodoRepo;
import com.vikasr.todo.Service.AnalyticsService;
import com.vikasr.todo.Service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TodoSchedulingIntegrationTests {

    @Autowired
    private TodoService todoService;

    @Autowired
    private TodoRepo todoRepo;

    @Autowired
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        todoRepo.deleteAll();
    }

    @Test
    void createTodoComputesReminderFromDueTimeAndOffset() {
        LocalDateTime dueTime = LocalDateTime.of(2026, 3, 30, 18, 0);

        TodoRequestDTO request = new TodoRequestDTO();
        request.setTitle("Submit report");
        request.setDescription("Send final report");
        request.setEmail("test@example.com");
        request.setDueTime(dueTime);
        request.setReminderMinutesBefore(90);

        TodoResponseDTO response = todoService.createTodo(request);

        assertEquals(dueTime, response.getDueTime());
        assertEquals(dueTime.minusMinutes(90), response.getReminderTime());
        assertEquals(90, response.getReminderMinutesBefore());
    }

    @Test
    void createTodoRejectsReminderOffsetWithoutDueTime() {
        TodoRequestDTO request = new TodoRequestDTO();
        request.setTitle("Submit report");
        request.setDescription("Send final report");
        request.setEmail("test@example.com");
        request.setReminderMinutesBefore(15);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> todoService.createTodo(request));
        assertEquals("Due time is required when reminderMinutesBefore is provided", exception.getMessage());
    }

    @Test
    void analyticsUseDueTimeForOverdueCalculation() {
        Todo todo = new Todo();
        todo.setTitle("Late task");
        todo.setDescription("Pending");
        todo.setEmail("test@example.com");
        todo.setCreatedAt(LocalDateTime.now().minusDays(2));
        todo.setDueTime(LocalDateTime.now().minusHours(1));
        todo.setReminderTime(LocalDateTime.now().plusHours(2));
        todo.setCompleted(false);
        todo.setArchived(false);
        todo.setReminderSent(false);
        todo.setOverdueNotificationSent(false);
        todoRepo.save(todo);

        TodoAnalyticsResponseDTO analytics = analyticsService.getTodoAnalytics();

        assertEquals(1, analytics.getOverdueTodos());
    }

    @Test
    void analyticsTreatArchivedTodosAsSeparateFromPendingAndCompleted() {
        Todo pendingTodo = new Todo();
        pendingTodo.setTitle("Pending");
        pendingTodo.setDescription("Pending");
        pendingTodo.setEmail("test@example.com");
        pendingTodo.setCreatedAt(LocalDateTime.now());
        pendingTodo.setCompleted(false);
        pendingTodo.setArchived(false);

        Todo completedTodo = new Todo();
        completedTodo.setTitle("Completed");
        completedTodo.setDescription("Completed");
        completedTodo.setEmail("test@example.com");
        completedTodo.setCreatedAt(LocalDateTime.now());
        completedTodo.setCompleted(true);
        completedTodo.setArchived(false);

        Todo archivedTodo = new Todo();
        archivedTodo.setTitle("Archived");
        archivedTodo.setDescription("Archived");
        archivedTodo.setEmail("test@example.com");
        archivedTodo.setCreatedAt(LocalDateTime.now());
        archivedTodo.setCompleted(false);
        archivedTodo.setArchived(true);

        todoRepo.save(pendingTodo);
        todoRepo.save(completedTodo);
        todoRepo.save(archivedTodo);

        TodoAnalyticsResponseDTO analytics = analyticsService.getTodoAnalytics();

        assertEquals(1, analytics.getPendingTodos());
        assertEquals(1, analytics.getCompletedTodos());
        assertEquals(1, analytics.getArchivedTodos());
    }

    @Test
    void markCompleteRejectsIncompleteDependencies() {
        Todo blocker = new Todo();
        blocker.setTitle("Blocker");
        blocker.setDescription("Finish first");
        blocker.setEmail("test@example.com");
        blocker.setCreatedAt(LocalDateTime.now());
        blocker.setCompleted(false);
        blocker.setArchived(false);
        Todo savedBlocker = todoRepo.save(blocker);

        TodoRequestDTO request = new TodoRequestDTO();
        request.setTitle("Blocked task");
        request.setDescription("Cannot finish yet");
        request.setEmail("test@example.com");
        request.setDependencyIds(Set.of(savedBlocker.getId()));

        TodoResponseDTO blockedTodo = todoService.createTodo(request);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> todoService.markComplete(blockedTodo.getId()));

        assertEquals("Todo is blocked by incomplete dependencies: Blocker", exception.getMessage());
        assertFalse(todoRepo.findById(blockedTodo.getId()).orElseThrow().isCompleted());
    }

    @Test
    void markCompleteCreatesNextRecurringTodoWithShiftedSchedule() {
        LocalDateTime dueTime = LocalDateTime.of(2026, 4, 1, 9, 0);

        TodoRequestDTO request = new TodoRequestDTO();
        request.setTitle("Standup prep");
        request.setDescription("Prepare notes");
        request.setEmail("test@example.com");
        request.setDueTime(dueTime);
        request.setReminderMinutesBefore(30);
        request.setRecurrencePattern(RecurrencePattern.DAILY);
        request.setRecurrenceInterval(2);

        TodoResponseDTO createdTodo = todoService.createTodo(request);

        todoService.markComplete(createdTodo.getId());

        assertEquals(2, todoRepo.count());

        Todo completedTodo = todoRepo.findById(createdTodo.getId()).orElseThrow();
        assertTrue(completedTodo.isCompleted());

        Todo nextTodo = todoRepo.findAll().stream()
                .filter(todo -> !todo.getId().equals(createdTodo.getId()))
                .findFirst()
                .orElseThrow();

        assertFalse(nextTodo.isCompleted());
        assertEquals(dueTime.plusDays(2), nextTodo.getDueTime());
        assertEquals(dueTime.plusDays(2).minusMinutes(30), nextTodo.getReminderTime());
        assertEquals(30, nextTodo.getReminderMinutesBefore());
        assertEquals(RecurrencePattern.DAILY, nextTodo.getRecurrencePattern());
        assertEquals(2, nextTodo.getRecurrenceInterval());
    }

    @Test
    void updateTodoRejectsDependencyCycles() {
        TodoRequestDTO firstRequest = new TodoRequestDTO();
        firstRequest.setTitle("Task A");
        firstRequest.setDescription("A");
        firstRequest.setEmail("test@example.com");
        TodoResponseDTO firstTodo = todoService.createTodo(firstRequest);

        TodoRequestDTO secondRequest = new TodoRequestDTO();
        secondRequest.setTitle("Task B");
        secondRequest.setDescription("B");
        secondRequest.setEmail("test@example.com");
        secondRequest.setDependencyIds(Set.of(firstTodo.getId()));
        TodoResponseDTO secondTodo = todoService.createTodo(secondRequest);

        TodoRequestDTO updateFirst = new TodoRequestDTO();
        updateFirst.setTitle("Task A");
        updateFirst.setDescription("A");
        updateFirst.setEmail("test@example.com");
        updateFirst.setDependencyIds(Set.of(secondTodo.getId()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> todoService.updateTodo(firstTodo.getId(), updateFirst));

        assertEquals("Dependency cycle detected", exception.getMessage());
    }
}
