package com.taskflow.category.service;

import com.taskflow.category.dto.*;
import com.taskflow.category.entity.Category;
import com.taskflow.category.repository.CategoryRepository;
import com.taskflow.common.exception.ApiException;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final UserRepository users;
    private final TaskRepository tasks;

    public CategoryService(CategoryRepository categories, UserRepository users, TaskRepository tasks) {
        this.categories = categories; this.users = users; this.tasks = tasks;
    }

    @Transactional
    public CategoryResponse create(UUID userId, CategoryRequest request) {
        if (categories.existsByUser_IdAndNameIgnoreCase(userId, request.name().trim()))
            throw ApiException.conflict("A category with this name already exists.");
        var user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        return toResponse(categories.save(new Category(user, request.name())));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return categories.findAllByUser_IdOrderByNameAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID id, CategoryRequest request) {
        Category category = categories.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found."));
        if (!category.getName().equalsIgnoreCase(request.name().trim())
                && categories.existsByUser_IdAndNameIgnoreCase(userId, request.name().trim()))
            throw ApiException.conflict("A category with this name already exists.");
        category.rename(request.name());
        return toResponse(category);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Category category = categories.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found."));
        if (tasks.existsByCategory_IdAndUser_IdAndDeletedAtIsNull(id, userId))
            throw ApiException.conflict("Category is assigned to active tasks. Reassign those tasks first.");
        categories.delete(category);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getCreatedAt());
    }
}
