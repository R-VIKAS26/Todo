package com.vikasr.todo.DTO;

import com.vikasr.todo.Model.TodoPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoViewRequestDTO {

    @NotBlank
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
}
