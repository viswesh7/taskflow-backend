package com.taskflow.category.entity;

import com.taskflow.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_categories_user_name",
        columnNames = {"user_id", "name"}))
public class Category {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Category() {}

    public Category(User user, String name) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = name.trim();
        this.createdAt = Instant.now();
    }

    public void rename(String name) { this.name = name.trim(); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
