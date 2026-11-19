package com.taskflow.task.entity;

import com.taskflow.category.entity.Category;
import com.taskflow.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Writable column used by Hibernate for INSERT and UPDATE
    @Column(name = "category_id")
    private UUID categoryId;

    // Read-only relationship used to retrieve Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = "category_id",
                    referencedColumnName = "id",
                    insertable = false,
                    updatable = false
            ),
            @JoinColumn(
                    name = "user_id",
                    referencedColumnName = "user_id",
                    insertable = false,
                    updatable = false
            )
    })
    private Category category;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Task() {
    }

    public Task(
            User user,
            Category category,
            String title,
            String description,
            TaskPriority priority,
            Instant dueDate
    ) {
        this.id = UUID.randomUUID();
        this.user = user;

        setCategory(category);

        this.title = title.trim();
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = TaskStatus.TODO;

        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;

        // Do not initialize version.
        // Hibernate manages @Version automatically.
    }

    public void update(
            Category category,
            String title,
            String description,
            TaskPriority priority,
            Instant dueDate
    ) {
        setCategory(category);

        this.title = title.trim();
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(TaskStatus next) {

        boolean allowed = switch (status) {
            case TODO ->
                    next == TaskStatus.IN_PROGRESS
                            || next == TaskStatus.COMPLETED
                            || next == TaskStatus.CANCELLED;

            case IN_PROGRESS ->
                    next == TaskStatus.TODO
                            || next == TaskStatus.COMPLETED
                            || next == TaskStatus.CANCELLED;

            case COMPLETED ->
                    next == TaskStatus.TODO;

            case CANCELLED ->
                    next == TaskStatus.TODO;
        };

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid task status transition: "
                            + status + " -> " + next
            );
        }

        this.status = next;

        this.completedAt =
                next == TaskStatus.COMPLETED
                        ? Instant.now()
                        : null;

        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
    }

    public void setCategory(Category category) {
        this.category = category;
        this.categoryId =
                category == null ? null : category.getId();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }
}