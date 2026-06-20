-- 1. Enable the pgvector extension (Required for AI Embeddings)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create Chat Sessions Table
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create Chat Messages Table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(50) NOT NULL,
    message_text TEXT NOT NULL,
    embedding vector(1536), 
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create an HNSW index for highly optimized similarity search
CREATE INDEX idx_chat_messages_embedding ON chat_messages USING hnsw (embedding vector_cosine_ops);