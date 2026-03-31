package com.vikasr.todo.DTO;


import com.vikasr.todo.Model.RecurrencePattern;
import com.vikasr.todo.Model.TodoPriority;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class TodoResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String email;
    private Long listId;
    private LocalDateTime dueTime;
    private LocalDateTime reminderTime;
    private Integer reminderMinutesBefore;
    private String category;
    private Set<String> tags;
    private Set<Long> dependencyIds;
    private List<String> blockedByTitles;
    private RecurrencePattern recurrencePattern;
    private Integer recurrenceInterval;
    private TodoPriority priority;
    private Long displayOrder;
    private boolean completed;
    private LocalDateTime createdAt;
    private Long trackedSeconds;
    private LocalDateTime timerStartedAt;
    private Boolean timerRunning;
    private Boolean archived;
    private Boolean reminderSent;
    private Boolean overdueNotificationSent;

}
