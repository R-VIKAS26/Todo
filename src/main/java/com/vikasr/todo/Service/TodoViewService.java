package com.vikasr.todo.Service;

import com.vikasr.todo.DTO.TodoViewRequestDTO;
import com.vikasr.todo.DTO.TodoViewResponseDTO;

import java.util.List;

public interface TodoViewService {

    TodoViewResponseDTO createView(TodoViewRequestDTO request);

    List<TodoViewResponseDTO> getAllViews();

    TodoViewResponseDTO updateView(Long id, TodoViewRequestDTO request);

    void deleteView(Long id);
}
