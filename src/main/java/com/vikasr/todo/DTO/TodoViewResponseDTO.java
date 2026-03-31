package com.vikasr.todo.DTO;

import com.vikasr.todo.Model.TodoPriority;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TodoViewResponseDTO {

    private Long id;
    private String name;
    private String search;
    private String category;
    private String tag;
    private TodoPriority priority;
    private Boolean completed;
    private Boolean archived;
    private String sortBy;
    private String direction;
    private Integer pageSize;
    private LocalDateTime createdAt;
}
