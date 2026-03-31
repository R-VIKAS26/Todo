package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.AnalyticsCountDTO;
import com.vikasr.todo.DTO.AnalyticsTrendPointDTO;
import com.vikasr.todo.DTO.TodoAnalyticsResponseDTO;
import com.vikasr.todo.Model.TodoPriority;
import com.vikasr.todo.Repository.TodoRepo;
import com.vikasr.todo.Service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TodoRepo todoRepo;

    @Override
    public TodoAnalyticsResponseDTO getTodoAnalytics() {
        long totalTodos = todoRepo.count();
        long completedTodos = todoRepo.countByCompletedTrueAndArchivedFalse();
        long pendingTodos = todoRepo.countByCompletedFalseAndArchivedFalse();
        long archivedTodos = todoRepo.countByArchivedTrue();
        long overdueTodos = todoRepo.countOverdueTodos(LocalDateTime.now());
        double completionRate = totalTodos == 0 ? 0.0 : (completedTodos * 100.0) / totalTodos;

        return TodoAnalyticsResponseDTO.builder()
                .totalTodos(totalTodos)
                .completedTodos(completedTodos)
                .pendingTodos(pendingTodos)
                .archivedTodos(archivedTodos)
                .overdueTodos(overdueTodos)
                .completionRate(completionRate)
                .priorityBreakdown(buildPriorityBreakdown())
                .createdByDay(buildCreatedByDayTrend())
                .build();
    }

    private List<AnalyticsCountDTO> buildPriorityBreakdown() {
        return List.of(
                new AnalyticsCountDTO("HIGH", todoRepo.countByPriority(TodoPriority.HIGH)),
                new AnalyticsCountDTO("MEDIUM", todoRepo.countByPriority(TodoPriority.MEDIUM)),
                new AnalyticsCountDTO("LOW", todoRepo.countByPriority(TodoPriority.LOW))
        );
    }

    private List<AnalyticsTrendPointDTO> buildCreatedByDayTrend() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        List<Object[]> rows = todoRepo.countTodosCreatedByDay(startDate.atStartOfDay());
        List<AnalyticsTrendPointDTO> trend = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            long count = rows.stream()
                    .filter(row -> date.equals(row[0]))
                    .map(row -> (Long) row[1])
                    .findFirst()
                    .orElse(0L);
            trend.add(new AnalyticsTrendPointDTO(date.toString(), count));
        }

        return trend;
    }
}
