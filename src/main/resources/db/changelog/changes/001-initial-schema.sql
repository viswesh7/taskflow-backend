--liquibase formatted sql

--changeset taskflow:001-create-users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

--changeset taskflow:002-create-categories
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT uk_categories_id_user UNIQUE (id, user_id)
);

--changeset taskflow:003-create-tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    due_date TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tasks_category_owner FOREIGN KEY (category_id, user_id)
        REFERENCES categories(id, user_id),
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

--changeset taskflow:004-create-refresh-tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

--changeset taskflow:005-create-indexes
CREATE INDEX idx_tasks_user_status_active ON tasks(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_user_due_active ON tasks(user_id, due_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_user_created_active ON tasks(user_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_user ON categories(user_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
