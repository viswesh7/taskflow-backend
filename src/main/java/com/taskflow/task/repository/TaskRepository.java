package com.taskflow.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.taskflow.task.entity.Task;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);
    boolean existsByCategory_IdAndUser_IdAndDeletedAtIsNull(UUID categoryId, UUID userId);

    @Query("select count(t) from Task t where t.user.id = :userId and t.deletedAt is null")
    long countActive(@Param("userId") UUID userId);

    @Query("select count(t) from Task t where t.user.id = :userId and t.deletedAt is null and t.status <> 'COMPLETED' and t.status <> 'CANCELLED'")
    long countPending(@Param("userId") UUID userId);

    @Query("select count(t) from Task t where t.user.id = :userId and t.deletedAt is null and t.status = 'COMPLETED'")
    long countCompleted(@Param("userId") UUID userId);

    @Query("select count(t) from Task t where t.user.id = :userId and t.deletedAt is null and t.status <> 'COMPLETED' and t.status <> 'CANCELLED' and t.dueDate < :now")
    long countOverdue(@Param("userId") UUID userId, @Param("now") Instant now);
}
