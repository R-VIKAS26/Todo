package com.vikasr.todo.Repository;

import com.vikasr.todo.Model.TodoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoViewRepo extends JpaRepository<TodoView, Long> {
}
