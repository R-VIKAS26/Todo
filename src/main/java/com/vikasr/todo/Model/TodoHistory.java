package com.vikasr.todo.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TodoHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TodoHistoryAction action;
    private Long todoId;
    @Lob
    private String beforeState;
    @Lob
    private String afterState;
    private Boolean undone = false;
    private LocalDateTime createdAt;
    private LocalDateTime undoneAt;
}
