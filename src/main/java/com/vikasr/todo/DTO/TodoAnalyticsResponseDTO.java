package com.vikasr.todo.DTO;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TodoAnalyticsResponseDTO {

    private long totalTodos;
    private long completedTodos;
    private long pendingTodos;
    private long archivedTodos;
    private long overdueTodos;
    private double completionRate;
    private List<AnalyticsCountDTO> priorityBreakdown;
    private List<AnalyticsTrendPointDTO> createdByDay;
}
