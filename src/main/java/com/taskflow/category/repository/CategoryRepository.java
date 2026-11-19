package com.taskflow.category.repository;

import com.taskflow.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByUser_IdOrderByNameAsc(UUID userId);
    Optional<Category> findByIdAndUser_Id(UUID id, UUID userId);
    boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);
}
