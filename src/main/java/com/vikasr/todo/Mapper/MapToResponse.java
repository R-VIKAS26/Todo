package com.vikasr.todo.Mapper;

import com.vikasr.todo.DTO.TodoResponseDTO;
import com.vikasr.todo.Model.Todo;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;


public class MapToResponse {
    public static TodoResponseDTO mapToResponse(Todo todo) {

        TodoResponseDTO dto = new TodoResponseDTO();

        dto.setId(todo.getId());
        dto.setTitle(todo.getTitle());
        dto.setDescription(todo.getDescription());
        dto.setEmail(todo.getEmail());
        dto.setListId(todo.getList() != null ? todo.getList().getId() : null);
        dto.setDueTime(todo.getDueTime());
        dto.setReminderTime(todo.getReminderTime());
        dto.setReminderMinutesBefore(todo.getReminderMinutesBefore());
        dto.setCategory(todo.getCategory());
        dto.setTags(new LinkedHashSet<>(todo.getTags()));
        dto.setDependencyIds(todo.getDependencies().stream()
                .map(Todo::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        dto.setBlockedByTitles(todo.getDependencies().stream()
                .filter(dependency -> !dependency.isCompleted())
                .map(Todo::getTitle)
                .toList());
        dto.setRecurrencePattern(todo.getRecurrencePattern());
        dto.setRecurrenceInterval(todo.getRecurrenceInterval());
        dto.setPriority(todo.getPriority());
        dto.setDisplayOrder(todo.getDisplayOrder());
        dto.setCompleted(todo.isCompleted());
        dto.setCreatedAt(todo.getCreatedAt());
        dto.setTrackedSeconds(todo.getTrackedSeconds());
        dto.setTimerStartedAt(todo.getTimerStartedAt());
        dto.setTimerRunning(todo.getTimerRunning());
        dto.setArchived(todo.getArchived());
        dto.setReminderSent(todo.getReminderSent());
        dto.setOverdueNotificationSent(todo.getOverdueNotificationSent());

        return dto;
    }
}
