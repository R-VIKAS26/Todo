package com.vikasr.todo.Repository;

import com.vikasr.todo.Model.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoListRepo extends JpaRepository<TodoList, Long> {

    @Query("""
            select distinct l
            from TodoList l
            left join l.sharedWithEmails sharedEmail
            where lower(l.ownerEmail) = lower(:email)
               or lower(sharedEmail) = lower(:email)
            order by l.createdAt desc
            """)
    List<TodoList> findAccessibleByEmail(@Param("email") String email);

    @Query("""
            select case when count(l) > 0 then true else false end
            from TodoList l
            left join l.sharedWithEmails sharedEmail
            where l.id = :listId
              and (lower(l.ownerEmail) = lower(:email)
                   or lower(sharedEmail) = lower(:email))
            """)
    boolean existsAccessibleByIdAndEmail(@Param("listId") Long listId, @Param("email") String email);

    Optional<TodoList> findByIdAndOwnerEmailIgnoreCase(Long id, String ownerEmail);
}
