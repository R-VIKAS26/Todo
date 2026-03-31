package com.vikasr.todo.Service;

import com.vikasr.todo.DTO.PagedTodoResponseDTO;
import com.vikasr.todo.DTO.TodoReorderRequestDTO;
import com.vikasr.todo.DTO.TodoRequestDTO;
import com.vikasr.todo.DTO.TodoResponseDTO;
import com.vikasr.todo.Model.TodoPriority;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface TodoService {

    TodoResponseDTO createTodo(TodoRequestDTO request);

    PagedTodoResponseDTO getAllTodo(Pageable pageable,
                                    String search,
                                    Long listId,
                                    String requesterEmail,
                                    String category,
                                    String tag,
                                    LocalDateTime fromDate,
                                    LocalDateTime toDate,
                                    TodoPriority priority,
                                    Boolean completed,
                                    Boolean archived);

    TodoResponseDTO updateTodo(Long id, TodoRequestDTO request);

    void deleteTodo(Long id, String requesterEmail);

    void markComplete(Long id);

    TodoResponseDTO startTimer(Long id, String requesterEmail);

    TodoResponseDTO pauseTimer(Long id, String requesterEmail);

    TodoResponseDTO stopTimer(Long id, String requesterEmail);

    TodoResponseDTO sendReminderEmail(Long id, String requesterEmail);

    void reorderTodos(TodoReorderRequestDTO request, String requesterEmail);

    void undoLastAction();

    void redoLastAction();
}
