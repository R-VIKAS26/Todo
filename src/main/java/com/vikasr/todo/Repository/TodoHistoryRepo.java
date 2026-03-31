package com.vikasr.todo.Repository;

import com.vikasr.todo.Model.TodoHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TodoHistoryRepo extends JpaRepository<TodoHistory, Long> {

    Optional<TodoHistory> findTopByUndoneFalseOrderByCreatedAtDesc();

    Optional<TodoHistory> findTopByUndoneTrueOrderByUndoneAtDesc();
}
