-- this script runs when the test database is created
-- Create a separate database for testing
-- CREATE DATABASE "spring-pgvector-test";

-- Connect to the test database
-- \c "spring-pgvector-test"

-- Enable the pgvector extension in the test database
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE IF NOT EXISTS vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content text,
	metadata json,
	embedding vector(768) -- 1536 is the default embedding dimension
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);

-- just a test to see when/if the script is run
create table if not exists test_init_table (
    id serial primary key,
    delete_this_table text
)
