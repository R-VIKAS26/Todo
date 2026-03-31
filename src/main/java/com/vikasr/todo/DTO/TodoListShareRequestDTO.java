package com.vikasr.todo.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoListShareRequestDTO {

    @NotBlank
    @Email
    private String ownerEmail;

    @NotBlank
    @Email
    private String memberEmail;
}
