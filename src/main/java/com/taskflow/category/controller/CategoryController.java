package com.taskflow.category.controller;

import com.taskflow.category.dto.*;
import com.taskflow.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(Authentication auth, @Valid @RequestBody CategoryRequest request) {
        return service.create(userId(auth), request);
    }
    @GetMapping
    public List<CategoryResponse> list(Authentication auth) { return service.list(userId(auth)); }
    @PutMapping("/{id}")
    public CategoryResponse update(Authentication auth, @PathVariable UUID id,
                                   @Valid @RequestBody CategoryRequest request) {
        return service.update(userId(auth), id, request);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable UUID id) { service.delete(userId(auth), id); }

    private UUID userId(Authentication auth) { return UUID.fromString(auth.getName()); }
}
