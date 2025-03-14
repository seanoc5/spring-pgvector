-- this script should be run automatically for the initial setup (may not be run if the database already exists, even un-initiated)
-- Enable the pgvector extension in the test database
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- this is typical spring-ai pgvector setup -- may be a better approach, like just pulling pgvector from dockerhub...?
CREATE TABLE IF NOT EXISTS vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content text,
	metadata json,
	embedding vector(1024) -- 1536 is the default embedding dimension
);
-- consider pros/cons of different index types, or if an index is even necessary
CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
