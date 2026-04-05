package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.PagedTodoResponseDTO;
import com.vikasr.todo.DTO.TodoReorderRequestDTO;
import com.vikasr.todo.DTO.TodoRequestDTO;
import com.vikasr.todo.DTO.TodoResponseDTO;
import com.vikasr.todo.DTO.TodoSnapshotDTO;
import com.vikasr.todo.Mapper.MapToResponse;
import com.vikasr.todo.Model.RecurrencePattern;
import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Model.TodoHistory;
import com.vikasr.todo.Model.TodoHistoryAction;
import com.vikasr.todo.Model.TodoList;
import com.vikasr.todo.Model.TodoPriority;
import com.vikasr.todo.Repository.TodoHistoryRepo;
import com.vikasr.todo.Repository.TodoListRepo;
import com.vikasr.todo.Repository.TodoRepo;
import com.vikasr.todo.Repository.TodoSpecification;
import com.vikasr.todo.Service.EmailService;
import com.vikasr.todo.Service.TodoService;
import com.vikasr.todo.Service.TodoListService;
import com.vikasr.todo.exception.DependencyCycleException;
import com.vikasr.todo.exception.DependencyNotFoundException;
import com.vikasr.todo.exception.InvalidReminderTimeException;
import com.vikasr.todo.exception.NegativeReminderMinutesException;
import com.vikasr.todo.exception.SelfDependencyException;
import com.vikasr.todo.exception.TodoNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.vikasr.todo.Mapper.MapToResponse.mapToResponse;

@Service
public class TodoServiceImpl implements TodoService {

    private final TodoRepo todoRepo;
    private final TodoHistoryRepo todoHistoryRepo;
    private final TodoListRepo todoListRepo;
    private final TodoListService todoListService;
    private final EmailService emailService;

    @Autowired
    public TodoServiceImpl(TodoRepo todoRepo,
                           TodoHistoryRepo todoHistoryRepo,
                           TodoListRepo todoListRepo,
                           TodoListService todoListService,
                           EmailService emailService) {
        this.todoRepo = todoRepo;
        this.todoHistoryRepo = todoHistoryRepo;
        this.todoListRepo = todoListRepo;
        this.todoListService = todoListService;
        this.emailService = emailService;
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @Override
    public TodoResponseDTO createTodo(TodoRequestDTO request) {

        Todo todo = new Todo();
        todo.setToken(UUID.randomUUID().toString());
        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        todo.setEmail(request.getEmail());
        todo.setList(resolveList(request.getListId(), request.getRequesterEmail()));
        applySchedule(todo, request);
        todo.setCategory(request.getCategory());
        todo.setTags(request.getTags() != null ? new LinkedHashSet<>(request.getTags()) : new LinkedHashSet<>());
        todo.setDependencies(resolveDependencies(request.getDependencyIds(), null));
        todo.setRecurrencePattern(request.getRecurrencePattern() != null ? request.getRecurrencePattern() : RecurrencePattern.NONE);
        todo.setRecurrenceInterval(normalizeRecurrenceInterval(request.getRecurrenceInterval()));
        todo.setPriority(request.getPriority() != null ? request.getPriority() : TodoPriority.MEDIUM);
        todo.setDisplayOrder(nextDisplayOrder());
        todo.setCompleted(Boolean.TRUE.equals(request.getCompleted()));
        todo.setArchived(Boolean.TRUE.equals(request.getArchived()));
        todo.setCreatedAt(LocalDateTime.now());
        todo.setTrackedSeconds(0L);
        todo.setTimerRunning(false);
        todo.setReminderSent(false);
        todo.setOverdueNotificationSent(false);

        Todo saved = todoRepo.save(todo);
        recordHistory(TodoHistoryAction.CREATE, saved.getId(), null, saved);
        messagingTemplate.convertAndSend("/topic/todos", "created");

        return mapToResponse(saved);
    }

    @Override
    public PagedTodoResponseDTO getAllTodo(Pageable pageable,
                                           String search,
                                           Long listId,
                                           String requesterEmail,
                                           String category,
                                           String tag,
                                           LocalDateTime fromDate,
                                           LocalDateTime toDate,
                                           TodoPriority priority,
                                           Boolean completed,
                                           Boolean archived) {
        if (listId != null) {
            todoListService.getAccessibleList(listId, requesterEmail);
        }

        Page<TodoResponseDTO> todoPage = todoRepo.findAll(
                        TodoSpecification.withFilters(search, listId, category, tag, fromDate, toDate, priority, completed, archived),
                        pageable)
                .map(MapToResponse::mapToResponse);

        return PagedTodoResponseDTO.builder()
                .content(todoPage.getContent())
                .page(todoPage.getNumber())
                .size(todoPage.getSize())
                .totalElements(todoPage.getTotalElements())
                .totalPages(todoPage.getTotalPages())
                .last(todoPage.isLast())
                .build();

    }


    @Override
    public TodoResponseDTO updateTodo(Long id, TodoRequestDTO request) {

        Todo todo = todoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        TodoSnapshotDTO before = snapshot(todo);

        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        todo.setEmail(request.getEmail());
        validateTodoAccess(todo, request.getRequesterEmail());
        todo.setList(resolveList(request.getListId(), request.getRequesterEmail()));
        applySchedule(todo, request);
        todo.setCategory(request.getCategory());
        todo.setTags(request.getTags() != null ? new LinkedHashSet<>(request.getTags()) : new LinkedHashSet<>());
        todo.setDependencies(resolveDependencies(request.getDependencyIds(), id));
        todo.setRecurrencePattern(request.getRecurrencePattern() != null ? request.getRecurrencePattern() : RecurrencePattern.NONE);
        todo.setRecurrenceInterval(normalizeRecurrenceInterval(request.getRecurrenceInterval()));
        todo.setPriority(request.getPriority() != null ? request.getPriority() : TodoPriority.MEDIUM);
        todo.setCompleted(Boolean.TRUE.equals(request.getCompleted()));
        todo.setArchived(Boolean.TRUE.equals(request.getArchived()));
        todo.setReminderSent(false);
        todo.setOverdueNotificationSent(false);

        messagingTemplate.convertAndSend("/topic/todos", "created");
        Todo saved = todoRepo.save(todo);
        recordHistory(TodoHistoryAction.UPDATE, saved.getId(), before, saved);

        return mapToResponse(saved);
    }


    @Override
    public void deleteTodo(Long id, String requesterEmail) {
        Todo todo = todoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        validateTodoAccess(todo, requesterEmail);
        TodoSnapshotDTO before = snapshot(todo);
        todoRepo.deleteById(id);
        recordHistory(TodoHistoryAction.DELETE, id, before, null);
        messagingTemplate.convertAndSend("/topic/todos", "created");

    }

    @Override
    @Transactional
    public void markComplete(Long id) {
        Todo todo = todoRepo.findById(id).orElseThrow();
        validateDependenciesCompleted(todo);
        accumulateTrackedTime(todo);
        todo.setTimerRunning(false);
        todo.setTimerStartedAt(null);
        todo.setCompleted(true);
        todoRepo.save(todo);
        createNextRecurringTodo(todo);
        messagingTemplate.convertAndSend("/topic/todos", "created");

    }

    @Override
    public TodoResponseDTO startTimer(Long id, String requesterEmail) {
        Todo todo = todoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        validateTodoAccess(todo, requesterEmail);

        if (Boolean.TRUE.equals(todo.getTimerRunning())) {
            return mapToResponse(todo);
        }

        todo.setTimerStartedAt(LocalDateTime.now());
        todo.setTimerRunning(true);
        Todo saved = todoRepo.save(todo);
        messagingTemplate.convertAndSend("/topic/todos", "created");
        return mapToResponse(saved);
    }

    @Override
    public TodoResponseDTO pauseTimer(Long id, String requesterEmail) {
        Todo todo = todoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        validateTodoAccess(todo, requesterEmail);

        accumulateTrackedTime(todo);
        todo.setTimerRunning(false);
        todo.setTimerStartedAt(null);
        Todo saved = todoRepo.save(todo);
        messagingTemplate.convertAndSend("/topic/todos", "created");
        return mapToResponse(saved);
    }

    @Override
    public TodoResponseDTO stopTimer(Long id, String requesterEmail) {
        return pauseTimer(id, requesterEmail);
    }

    @Override
    public TodoResponseDTO sendReminderEmail(Long id, String requesterEmail) {
        Todo todo = todoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        validateTodoAccess(todo, requesterEmail);

        if (!emailService.isMailDeliveryEnabled()) {
            throw new RuntimeException("Mail delivery is not configured");
        }
        if (todo.getEmail() == null || todo.getEmail().isBlank()) {
            throw new RuntimeException("Todo email is required to send a reminder");
        }
        if (todo.getToken() == null || todo.getToken().isBlank()) {
            todo.setToken(UUID.randomUUID().toString());
        }

        emailService.sendReminderEmail(
                todo.getEmail(),
                "Todo Reminder",
                todo.getTitle(),
                todo.getDescription(),
                todo.getId(),
                todo.getToken()
        ).join();

        todo.setReminderSent(true);
        Todo saved = todoRepo.save(todo);
        messagingTemplate.convertAndSend("/topic/todos", "created");
        return mapToResponse(saved);
    }

    @Override
    public void reorderTodos(TodoReorderRequestDTO request, String requesterEmail) {
        List<Todo> todos = todoRepo.findAllById(request.getTodoIds());
        if (todos.size() != request.getTodoIds().size()) {
            throw new RuntimeException("One or more todos not found for reordering");
        }
        todos.forEach(todo -> validateTodoAccess(todo, requesterEmail));

        for (int index = 0; index < request.getTodoIds().size(); index++) {
            Long todoId = request.getTodoIds().get(index);
            Todo todo = todos.stream()
                    .filter(item -> item.getId().equals(todoId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Todo not found for reordering"));
            todo.setDisplayOrder((long) index);
        }

        todoRepo.saveAll(todos);
        messagingTemplate.convertAndSend("/topic/todos", "created");
    }

    @Override
    public void undoLastAction() {
        TodoHistory history = todoHistoryRepo.findTopByUndoneFalseOrderByCreatedAtDesc()
                .orElseThrow(() -> new RuntimeException("No action available to undo"));

        switch (history.getAction()) {
            case CREATE -> todoRepo.deleteById(history.getTodoId());
            case UPDATE -> restoreSnapshot(readSnapshot(history.getBeforeState()));
            case DELETE -> {
                Todo restoredTodo = restoreSnapshot(readSnapshot(history.getBeforeState()));
                history.setTodoId(restoredTodo.getId());
                history.setBeforeState(writeSnapshot(snapshot(restoredTodo)));
            }
        }

        history.setUndone(true);
        history.setUndoneAt(LocalDateTime.now());
        todoHistoryRepo.save(history);
        messagingTemplate.convertAndSend("/topic/todos", "created");
    }

    @Override
    public void redoLastAction() {
        TodoHistory history = todoHistoryRepo.findTopByUndoneTrueOrderByUndoneAtDesc()
                .orElseThrow(() -> new RuntimeException("No action available to redo"));

        switch (history.getAction()) {
            case CREATE -> {
                Todo restoredTodo = restoreSnapshot(readSnapshot(history.getAfterState()));
                history.setTodoId(restoredTodo.getId());
                history.setAfterState(writeSnapshot(snapshot(restoredTodo)));
            }
            case UPDATE -> restoreSnapshot(readSnapshot(history.getAfterState()));
            case DELETE -> todoRepo.deleteById(history.getTodoId());
        }

        history.setUndone(false);
        history.setUndoneAt(null);
        todoHistoryRepo.save(history);
        messagingTemplate.convertAndSend("/topic/todos", "created");
    }

    private void createNextRecurringTodo(Todo todo) {
        if (todo.getRecurrencePattern() == null || todo.getRecurrencePattern() == RecurrencePattern.NONE) {
            return;
        }
        if (todo.getDueTime() == null && todo.getReminderTime() == null) {
            return;
        }

        Todo nextTodo = new Todo();
        nextTodo.setToken(UUID.randomUUID().toString());
        nextTodo.setTitle(todo.getTitle());
        nextTodo.setDescription(todo.getDescription());
        nextTodo.setEmail(todo.getEmail());
        nextTodo.setList(todo.getList());
        TodoRequestDTO scheduleRequest = new TodoRequestDTO();
        scheduleRequest.setDueTime(calculateNextScheduleTime(todo.getDueTime(), todo.getRecurrencePattern(), todo.getRecurrenceInterval()));
        scheduleRequest.setReminderTime(calculateNextScheduleTime(todo.getReminderTime(), todo.getRecurrencePattern(), todo.getRecurrenceInterval()));
        scheduleRequest.setReminderMinutesBefore(todo.getReminderMinutesBefore());
        applySchedule(nextTodo, scheduleRequest);
        nextTodo.setCategory(todo.getCategory());
        nextTodo.setTags(new LinkedHashSet<>(todo.getTags()));
        nextTodo.setDependencies(new LinkedHashSet<>(todo.getDependencies()));
        nextTodo.setRecurrencePattern(todo.getRecurrencePattern());
        nextTodo.setRecurrenceInterval(normalizeRecurrenceInterval(todo.getRecurrenceInterval()));
        nextTodo.setPriority(todo.getPriority());
        nextTodo.setDisplayOrder(nextDisplayOrder());
        nextTodo.setCompleted(false);
        nextTodo.setArchived(false);
        nextTodo.setReminderSent(false);
        nextTodo.setOverdueNotificationSent(false);
        nextTodo.setCreatedAt(LocalDateTime.now());

        todoRepo.save(nextTodo);
    }

    private LocalDateTime calculateNextScheduleTime(LocalDateTime scheduleTime,
                                                    RecurrencePattern recurrencePattern,
                                                    Integer recurrenceInterval) {
        if (scheduleTime == null) {
            return null;
        }
        int interval = normalizeRecurrenceInterval(recurrenceInterval);
        return switch (recurrencePattern) {
            case DAILY -> scheduleTime.plusDays(interval);
            case WEEKLY -> scheduleTime.plusWeeks(interval);
            case MONTHLY -> scheduleTime.plusMonths(interval);
            case NONE -> scheduleTime;
        };
    }

    private int normalizeRecurrenceInterval(Integer recurrenceInterval) {
        return recurrenceInterval == null || recurrenceInterval < 1 ? 1 : recurrenceInterval;
    }

    private long nextDisplayOrder() {
        return todoRepo.findTopByOrderByDisplayOrderDesc()
                .map(todo -> todo.getDisplayOrder() != null ? todo.getDisplayOrder() + 1 : 0L)
                .orElse(0L);
    }

    private void recordHistory(TodoHistoryAction action, Long todoId, TodoSnapshotDTO before, Todo after) {
        TodoHistory history = new TodoHistory();
        history.setAction(action);
        history.setTodoId(todoId);
        history.setBeforeState(writeSnapshot(before));
        history.setAfterState(writeSnapshot(after != null ? snapshot(after) : null));
        history.setCreatedAt(LocalDateTime.now());
        history.setUndone(false);
        todoHistoryRepo.save(history);
    }

    @Transactional
    private TodoSnapshotDTO snapshot(Todo todo) {
        TodoSnapshotDTO snapshot = new TodoSnapshotDTO();
        snapshot.setId(todo.getId());
        snapshot.setTitle(todo.getTitle());
        snapshot.setDescription(todo.getDescription());
        snapshot.setEmail(todo.getEmail());
        snapshot.setListId(todo.getList() != null ? todo.getList().getId() : null);
        snapshot.setDueTime(todo.getDueTime());
        snapshot.setReminderTime(todo.getReminderTime());
        snapshot.setReminderMinutesBefore(todo.getReminderMinutesBefore());
        snapshot.setCategory(todo.getCategory());
        snapshot.setTags(new LinkedHashSet<>(todo.getTags()));
        snapshot.setDependencyIds(todo.getDependencies().stream()
                .map(Todo::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        snapshot.setRecurrencePattern(todo.getRecurrencePattern());
        snapshot.setRecurrenceInterval(todo.getRecurrenceInterval());
        snapshot.setPriority(todo.getPriority());
        snapshot.setDisplayOrder(todo.getDisplayOrder());
        snapshot.setCompleted(todo.isCompleted());
        snapshot.setCreatedAt(todo.getCreatedAt());
        snapshot.setTrackedSeconds(todo.getTrackedSeconds());
        snapshot.setTimerStartedAt(todo.getTimerStartedAt());
        snapshot.setTimerRunning(todo.getTimerRunning());
        snapshot.setArchived(todo.getArchived());
        snapshot.setReminderSent(todo.getReminderSent());
        snapshot.setOverdueNotificationSent(todo.getOverdueNotificationSent());
        snapshot.setToken(todo.getToken());
        return snapshot;
    }

    private String writeSnapshot(TodoSnapshotDTO snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(snapshot);
            objectStream.flush();
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize todo history snapshot", e);
        }
    }

    private TodoSnapshotDTO readSnapshot(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (TodoSnapshotDTO) objectStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize todo history snapshot", e);
        }
    }

    private Todo restoreSnapshot(TodoSnapshotDTO snapshot) {
        if (snapshot == null) {
            return null;
        }

        Todo todo = todoRepo.findById(snapshot.getId()).orElse(null);
        boolean existingTodo = todo != null;
        if (todo == null) {
            todo = new Todo();
        }
        if (existingTodo) {
            todo.setId(snapshot.getId());
        }
        todo.setTitle(snapshot.getTitle());
        todo.setDescription(snapshot.getDescription());
        todo.setEmail(snapshot.getEmail());
        todo.setList(resolveList(snapshot.getListId()));
        todo.setDueTime(snapshot.getDueTime());
        todo.setReminderTime(snapshot.getReminderTime());
        todo.setReminderMinutesBefore(snapshot.getReminderMinutesBefore());
        todo.setCategory(snapshot.getCategory());
        todo.setTags(snapshot.getTags() != null ? new LinkedHashSet<>(snapshot.getTags()) : new LinkedHashSet<>());
        todo.setDependencies(resolveDependencies(snapshot.getDependencyIds(), snapshot.getId()));
        todo.setRecurrencePattern(snapshot.getRecurrencePattern());
        todo.setRecurrenceInterval(snapshot.getRecurrenceInterval());
        todo.setPriority(snapshot.getPriority());
        todo.setDisplayOrder(snapshot.getDisplayOrder());
        todo.setCompleted(snapshot.isCompleted());
        todo.setCreatedAt(snapshot.getCreatedAt());
        todo.setTrackedSeconds(snapshot.getTrackedSeconds());
        todo.setTimerStartedAt(snapshot.getTimerStartedAt());
        todo.setTimerRunning(snapshot.getTimerRunning());
        todo.setArchived(snapshot.getArchived());
        todo.setReminderSent(snapshot.getReminderSent());
        todo.setOverdueNotificationSent(snapshot.getOverdueNotificationSent());
        todo.setToken(snapshot.getToken());
        return todoRepo.save(todo);
    }

    private void applySchedule(Todo todo, TodoRequestDTO request) {
        LocalDateTime dueTime = request.getDueTime();
        LocalDateTime reminderTime = request.getReminderTime();
        Integer reminderMinutesBefore = normalizeReminderMinutesBefore(request.getReminderMinutesBefore());

        if (dueTime == null && reminderMinutesBefore != null) {
            throw new RuntimeException("Due time is required when reminderMinutesBefore is provided");
        }

        if (dueTime != null && reminderMinutesBefore != null) {
            reminderTime = dueTime.minusMinutes(reminderMinutesBefore);
        } else if (dueTime != null && reminderTime == null) {
            reminderTime = dueTime;
            reminderMinutesBefore = 0;
        } else if (dueTime != null) {
            long computedMinutes = java.time.temporal.ChronoUnit.MINUTES.between(reminderTime, dueTime);
            if (computedMinutes < 0) {
                throw new InvalidReminderTimeException("Reminder time cannot be after due time");
            }
            reminderMinutesBefore = Math.toIntExact(computedMinutes);
        }

        todo.setDueTime(dueTime);
        todo.setReminderTime(reminderTime);
        todo.setReminderMinutesBefore(reminderMinutesBefore);
    }

    private Integer normalizeReminderMinutesBefore(Integer reminderMinutesBefore) {
        if (reminderMinutesBefore == null) {
            return null;
        }
        if (reminderMinutesBefore < 0) {
            throw new NegativeReminderMinutesException("reminderMinutesBefore cannot be negative");
        }
        return reminderMinutesBefore;
    }

    private Set<Todo> resolveDependencies(Set<Long> dependencyIds, Long currentTodoId) {
        if (dependencyIds == null || dependencyIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        if (currentTodoId != null && dependencyIds.contains(currentTodoId)) {
            throw new SelfDependencyException("Todo cannot depend on itself");
        }

        List<Todo> dependencies = todoRepo.findAllById(dependencyIds);
        if (dependencies.size() != dependencyIds.size()) {
            throw new DependencyNotFoundException("One or more dependencies do not exist");
        }

        Set<Todo> resolved = dependencies.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (currentTodoId != null && createsCycle(currentTodoId, resolved)) {
            throw new DependencyCycleException("Dependency cycle detected");
        }

        return resolved;
    }

    private TodoList resolveList(Long listId, String requesterEmail) {
        return listId == null ? null : todoListService.getAccessibleList(listId, requesterEmail);
    }

    private TodoList resolveList(Long listId) {
        if (listId == null) {
            return null;
        }
        return todoListRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("Todo list not found"));
    }

    private void validateTodoAccess(Todo todo, String requesterEmail) {
        if (todo.getList() == null) {
            return;
        }
        todoListService.getAccessibleList(todo.getList().getId(), requesterEmail);
    }

    private boolean createsCycle(Long currentTodoId, Set<Todo> dependencies) {
        return dependencies.stream().anyMatch(dependency -> dependsOn(dependency, currentTodoId, new LinkedHashSet<>()));
    }

    private boolean dependsOn(Todo start, Long targetId, Set<Long> visited) {
        if (start.getId().equals(targetId)) {
            return true;
        }
        if (!visited.add(start.getId())) {
            return false;
        }

        return start.getDependencies().stream()
                .anyMatch(dependency -> dependsOn(dependency, targetId, visited));
    }

    private void validateDependenciesCompleted(Todo todo) {
        List<String> blockers = todo.getDependencies().stream()
                .filter(dependency -> !dependency.isCompleted())
                .map(Todo::getTitle)
                .toList();

        if (!blockers.isEmpty()) {
            throw new RuntimeException("Todo is blocked by incomplete dependencies: " + String.join(", ", blockers));
        }
    }

    private void accumulateTrackedTime(Todo todo) {
        if (!Boolean.TRUE.equals(todo.getTimerRunning()) || todo.getTimerStartedAt() == null) {
            return;
        }

        long alreadyTracked = todo.getTrackedSeconds() != null ? todo.getTrackedSeconds() : 0L;
        long currentSessionSeconds = Math.max(0, Duration.between(todo.getTimerStartedAt(), LocalDateTime.now()).getSeconds());
        todo.setTrackedSeconds(alreadyTracked + currentSessionSeconds);
    }

}
