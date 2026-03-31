package com.vikasr.todo.Service;

import com.vikasr.todo.DTO.TodoRequestDTO;
import com.vikasr.todo.DTO.TodoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface ExcelService {

    void exportTodos(HttpServletResponse response, List<TodoResponseDTO> todos);

    void importTodos(MultipartFile file);
}