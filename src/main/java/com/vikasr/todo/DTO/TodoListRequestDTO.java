package com.vikasr.todo.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoListRequestDTO {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String ownerEmail;
}
