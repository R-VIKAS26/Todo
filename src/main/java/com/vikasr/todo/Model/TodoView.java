package com.vikasr.todo.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TodoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String search;
    private String category;
    private String tag;
    @Enumerated(EnumType.STRING)
    private TodoPriority priority;
    private Boolean completed;
    private Boolean archived;
    private String sortBy;
    private String direction;
    private Integer pageSize;
    private LocalDateTime createdAt;
}
