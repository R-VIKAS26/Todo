package com.vikasr.todo.Repository;

import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Model.TodoPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepo extends JpaRepository<Todo,Long>, JpaSpecificationExecutor<Todo> {

        long countByReminderTimeBeforeAndCompletedFalseAndReminderSentFalse(LocalDateTime time);
        long countByCompletedTrueAndArchivedFalse();
        long countByCompletedFalseAndArchivedFalse();
        long countByArchivedTrue();
        long countByPriority(TodoPriority priority);

        @Query("""
                select count(t)
                from Todo t
                where coalesce(t.dueTime, t.reminderTime) < :time
                  and t.completed = false
                  and (t.archived = false or t.archived is null)
                """)
        long countOverdueTodos(LocalDateTime time);

        @Query("""
                select cast(t.createdAt as LocalDate), count(t)
                from Todo t
                where t.createdAt >= :fromDateTime
                group by cast(t.createdAt as LocalDate)
                order by cast(t.createdAt as LocalDate)
                """)
        List<Object[]> countTodosCreatedByDay(LocalDateTime fromDateTime);
        Optional<Todo> findTopByOrderByDisplayOrderDesc();
}
