package com.vikasr.todo.Service;

import com.vikasr.todo.DTO.TodoListRequestDTO;
import com.vikasr.todo.DTO.TodoListResponseDTO;
import com.vikasr.todo.DTO.TodoListShareRequestDTO;
import com.vikasr.todo.Model.TodoList;

import java.util.List;

public interface TodoListService {

    TodoListResponseDTO createList(TodoListRequestDTO request);

    List<TodoListResponseDTO> getAccessibleLists(String email);

    TodoListResponseDTO shareList(Long listId, TodoListShareRequestDTO request);

    TodoList getAccessibleList(Long listId, String requesterEmail);
}
