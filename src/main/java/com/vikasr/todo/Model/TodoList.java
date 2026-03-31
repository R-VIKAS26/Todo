package com.vikasr.todo.Model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class TodoList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String ownerEmail;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "todo_list_shared_emails")
    @Column(name = "email")
    private Set<String> sharedWithEmails = new LinkedHashSet<>();

    private LocalDateTime createdAt;
}
