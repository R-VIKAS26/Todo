package com.vikasr.todo.Repository;

import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Model.TodoPriority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class TodoSpecification {

    private TodoSpecification() {
    }

    public static Specification<Todo> withFilters(String search,
                                                  Long listId,
                                                  String category,
                                                  String tag,
                                                  LocalDateTime fromDate,
                                                  LocalDateTime toDate,
                                                  TodoPriority priority,
                                                  Boolean completed,
                                                  Boolean archived) {
        return Specification.allOf(
                hasSearch(search),
                hasListId(listId),
                hasCategory(category),
                hasTag(tag),
                hasDateRange(fromDate, toDate),
                hasPriority(priority),
                hasCompleted(completed),
                hasArchived(archived)
        );
    }

    private static Specification<Todo> hasListId(Long listId) {
        return (root, query, criteriaBuilder) ->
                listId == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("list").get("id"), listId);
    }

    private static Specification<Todo> hasSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)
            );
        };
    }

    private static Specification<Todo> hasPriority(TodoPriority priority) {
        return (root, query, criteriaBuilder) ->
                priority == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("priority"), priority);
    }

    private static Specification<Todo> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null || category.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.trim().toLowerCase());
        };
    }

    private static Specification<Todo> hasTag(String tag) {
        return (root, query, criteriaBuilder) -> {
            if (tag == null || tag.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            query.distinct(true);
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.join("tags").as(String.class)),
                    tag.trim().toLowerCase()
            );
        };
    }

    private static Specification<Todo> hasCompleted(Boolean completed) {
        return (root, query, criteriaBuilder) ->
                completed == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("completed"), completed);
    }

    private static Specification<Todo> hasDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, criteriaBuilder) -> {
            var scheduleTime = criteriaBuilder.<LocalDateTime>coalesce()
                    .value(root.get("dueTime"))
                    .value(root.get("reminderTime"));
            if (fromDate != null && toDate != null) {
                return criteriaBuilder.between(scheduleTime, fromDate, toDate);
            }
            if (fromDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(scheduleTime, fromDate);
            }
            if (toDate != null) {
                return criteriaBuilder.lessThanOrEqualTo(scheduleTime, toDate);
            }
            return criteriaBuilder.conjunction();
        };
    }

    private static Specification<Todo> hasArchived(Boolean archived) {
        return (root, query, criteriaBuilder) ->
                archived == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("archived"), archived);
    }
}
