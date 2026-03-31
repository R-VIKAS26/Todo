package com.vikasr.todo.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TodoListResponseDTO {

    private Long id;
    private String name;
    private String ownerEmail;
    private Set<String> sharedWithEmails;
    private LocalDateTime createdAt;
}
