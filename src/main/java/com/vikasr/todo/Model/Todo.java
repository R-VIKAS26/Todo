package com.vikasr.todo.Model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String email;
    private LocalDateTime dueTime;
    private LocalDateTime reminderTime;
    private Integer reminderMinutesBefore;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private TodoList list;
    private String category;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "todo_tags", joinColumns = @JoinColumn(name = "todo_id"))
    @Column(name = "tag")
    private Set<String> tags = new LinkedHashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "todo_dependencies",
            joinColumns = @JoinColumn(name = "todo_id"),
            inverseJoinColumns = @JoinColumn(name = "dependency_id")
    )
    private Set<Todo> dependencies = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING)
    private RecurrencePattern recurrencePattern = RecurrencePattern.NONE;
    private Integer recurrenceInterval = 1;
    @Enumerated(EnumType.STRING)
    private TodoPriority priority = TodoPriority.MEDIUM;
    private Long displayOrder;
    private boolean completed=false;
    private LocalDateTime createdAt;
    private Long trackedSeconds = 0L;
    private LocalDateTime timerStartedAt;
    private Boolean timerRunning = false;

    private Boolean archived = false;
    private Boolean reminderSent = false;
    private Boolean overdueNotificationSent = false;

    @Column
    private String token;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Todo todo = (Todo) o;
        return id != null && id.equals(todo.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
