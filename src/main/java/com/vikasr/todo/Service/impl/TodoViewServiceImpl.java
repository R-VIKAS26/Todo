package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.TodoViewRequestDTO;
import com.vikasr.todo.DTO.TodoViewResponseDTO;
import com.vikasr.todo.Model.TodoView;
import com.vikasr.todo.Repository.TodoViewRepo;
import com.vikasr.todo.Service.TodoViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoViewServiceImpl implements TodoViewService {

    private final TodoViewRepo todoViewRepo;

    @Override
    public TodoViewResponseDTO createView(TodoViewRequestDTO request) {
        TodoView view = new TodoView();
        applyRequest(view, request);
        view.setCreatedAt(LocalDateTime.now());
        return map(todoViewRepo.save(view));
    }

    @Override
    public List<TodoViewResponseDTO> getAllViews() {
        return todoViewRepo.findAll().stream()
                .map(this::map)
                .toList();
    }

    @Override
    public TodoViewResponseDTO updateView(Long id, TodoViewRequestDTO request) {
        TodoView view = todoViewRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Saved view not found"));
        applyRequest(view, request);
        return map(todoViewRepo.save(view));
    }

    @Override
    public void deleteView(Long id) {
        todoViewRepo.deleteById(id);
    }

    private void applyRequest(TodoView view, TodoViewRequestDTO request) {
        view.setName(request.getName());
        view.setSearch(request.getSearch());
        view.setCategory(request.getCategory());
        view.setTag(request.getTag());
        view.setPriority(request.getPriority());
        view.setCompleted(request.getCompleted());
        view.setArchived(request.getArchived());
        view.setSortBy(request.getSortBy() != null ? request.getSortBy() : "createdAt");
        view.setDirection(request.getDirection() != null ? request.getDirection() : "desc");
        view.setPageSize(request.getPageSize() != null ? request.getPageSize() : 10);
    }

    private TodoViewResponseDTO map(TodoView view) {
        return TodoViewResponseDTO.builder()
                .id(view.getId())
                .name(view.getName())
                .search(view.getSearch())
                .category(view.getCategory())
                .tag(view.getTag())
                .priority(view.getPriority())
                .completed(view.getCompleted())
                .archived(view.getArchived())
                .sortBy(view.getSortBy())
                .direction(view.getDirection())
                .pageSize(view.getPageSize())
                .createdAt(view.getCreatedAt())
                .build();
    }
}
