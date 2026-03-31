package com.vikasr.todo.DTO;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TodoReorderRequestDTO {

    @NotEmpty
    private List<Long> todoIds;
}
