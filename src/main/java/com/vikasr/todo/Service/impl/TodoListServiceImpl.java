package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.TodoListRequestDTO;
import com.vikasr.todo.DTO.TodoListResponseDTO;
import com.vikasr.todo.DTO.TodoListShareRequestDTO;
import com.vikasr.todo.Model.TodoList;
import com.vikasr.todo.Repository.TodoListRepo;
import com.vikasr.todo.Service.TodoListService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class TodoListServiceImpl implements TodoListService {

    private final TodoListRepo todoListRepo;

    public TodoListServiceImpl(TodoListRepo todoListRepo) {
        this.todoListRepo = todoListRepo;
    }

    @Override
    public TodoListResponseDTO createList(TodoListRequestDTO request) {
        TodoList todoList = new TodoList();
        todoList.setName(request.getName());
        todoList.setOwnerEmail(normalizeEmail(request.getOwnerEmail()));
        todoList.setSharedWithEmails(new LinkedHashSet<>());
        todoList.setCreatedAt(LocalDateTime.now());
        return mapToResponse(todoListRepo.save(todoList));
    }

    @Override
    public List<TodoListResponseDTO> getAccessibleLists(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        return todoListRepo.findAccessibleByEmail(normalizeEmail(email)).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TodoListResponseDTO shareList(Long listId, TodoListShareRequestDTO request) {
        TodoList todoList = todoListRepo.findByIdAndOwnerEmailIgnoreCase(listId, normalizeEmail(request.getOwnerEmail()))
                .orElseThrow(() -> new RuntimeException("Todo list not found or owner access denied"));

        String memberEmail = normalizeEmail(request.getMemberEmail());
        if (!memberEmail.equalsIgnoreCase(todoList.getOwnerEmail())) {
            todoList.getSharedWithEmails().add(memberEmail);
        }

        return mapToResponse(todoListRepo.save(todoList));
    }

    @Override
    public TodoList getAccessibleList(Long listId, String requesterEmail) {
        if (listId == null) {
            return null;
        }
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new RuntimeException("Requester email is required for shared lists");
        }
        if (!todoListRepo.existsAccessibleByIdAndEmail(listId, normalizeEmail(requesterEmail))) {
            throw new RuntimeException("Todo list access denied");
        }
        return todoListRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("Todo list not found"));
    }

    private TodoListResponseDTO mapToResponse(TodoList todoList) {
        TodoListResponseDTO response = new TodoListResponseDTO();
        response.setId(todoList.getId());
        response.setName(todoList.getName());
        response.setOwnerEmail(todoList.getOwnerEmail());
        response.setSharedWithEmails(new LinkedHashSet<>(todoList.getSharedWithEmails()));
        response.setCreatedAt(todoList.getCreatedAt());
        return response;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
