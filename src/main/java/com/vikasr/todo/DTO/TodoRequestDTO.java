package com.vikasr.todo.DTO;

import com.vikasr.todo.Model.RecurrencePattern;
import com.vikasr.todo.Model.TodoPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter

public class TodoRequestDTO {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private  String email;

    private Long listId;
    private String requesterEmail;
    private LocalDateTime dueTime;
    private LocalDateTime reminderTime;
    private Integer reminderMinutesBefore;
    private String category;
    private Set<String> tags;
    private Set<Long> dependencyIds;
    private RecurrencePattern recurrencePattern;
    private Integer recurrenceInterval;
    private TodoPriority priority;

    private Boolean completed;
    private Boolean archived;


}
