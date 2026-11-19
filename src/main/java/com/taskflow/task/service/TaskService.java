package com.taskflow.task.service;

import com.taskflow.category.entity.Category;
import com.taskflow.category.repository.CategoryRepository;
import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.ApiException;
import com.taskflow.task.dto.*;
import com.taskflow.task.entity.*;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.user.repository.UserRepository;
import com.taskflow.task.entity.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final UserRepository users;
    private final CategoryRepository categories;

    public TaskService(TaskRepository tasks, UserRepository users, CategoryRepository categories) {
        this.tasks = tasks; this.users = users; this.categories = categories;
    }

    @Transactional
    public TaskResponse create(UUID userId, TaskCreateRequest request) {
        var user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        Category category = resolveCategory(userId, request.categoryId());
        com.taskflow.task.entity.Task task1 = new Task(user, category, request.title(), request.description(),
                request.priority(), request.dueDate());
        return toResponse(tasks.save(task1));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID userId, UUID taskId) {
        return toResponse(findOwned(userId, taskId));
    }

    @Transactional
    public TaskResponse update(UUID userId, UUID taskId, TaskUpdateRequest request) {
        Task task = findOwned(userId, taskId);
        if (!Objects.equals(task.getVersion(), request.version())) {
            throw ApiException.conflict("Task was changed by another request. Reload before updating.");
        }
        task.update(resolveCategory(userId, request.categoryId()), request.title(),
                request.description(), request.priority(), request.dueDate());
        return toResponse(task);
    }

    @Transactional
    public TaskResponse changeStatus(UUID userId, UUID taskId, TaskStatusRequest request) {
        Task task = findOwned(userId, taskId);
        try {
            task.changeStatus(request.status());
        } catch (IllegalArgumentException ex) {
            throw ApiException.conflict(ex.getMessage());
        }
        return toResponse(task);
    }

    @Transactional
    public void delete(UUID userId, UUID taskId) {
        findOwned(userId, taskId).softDelete();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(UUID userId, TaskStatus status, TaskPriority priority,
            UUID categoryId, String search, Instant dueBefore, int page, int size,
            String sortBy, Sort.Direction direction) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.badRequest("Invalid page or size.");
        Set<String> allowedSort = Set.of("createdAt", "updatedAt", "dueDate", "priority", "title");
        if (!allowedSort.contains(sortBy)) throw ApiException.badRequest("Unsupported sort field.");
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Specification<com.taskflow.task.entity.Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority));
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (search != null && !search.isBlank())
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.trim().toLowerCase() + "%"));
            if (dueBefore != null) predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueBefore));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<TaskResponse> result = tasks.findAll(spec, pageable).map(this::toResponse);
        return PageResponse.from(result);
    }

    private Task findOwned(UUID userId, UUID taskId) {
        return tasks.findByIdAndUser_IdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> ApiException.notFound("Task not found."));
    }

    private Category resolveCategory(UUID userId, UUID categoryId) {
        if (categoryId == null) return null;
        return categories.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> ApiException.badRequest("Category does not exist or does not belong to this user."));
    }

    private TaskResponse toResponse(Task t) {
        boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(Instant.now())
                && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED;
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getPriority(), t.getDueDate(), t.getCompletedAt(), overdue,
                t.getCategory() == null ? null : t.getCategory().getId(),
                t.getCategory() == null ? null : t.getCategory().getName(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getVersion());
    }
}
