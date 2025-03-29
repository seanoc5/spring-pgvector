-- Initialize pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the naics_codes table if it doesn't exist
CREATE TABLE IF NOT EXISTS naics_codes (
    code VARCHAR(255) PRIMARY KEY,
    level INTEGER,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sector_code VARCHAR(255),
    sector_title VARCHAR(255),
    subsector_code VARCHAR(255),
    subsector_title VARCHAR(255),
    industry_group_code VARCHAR(255),
    industry_group_title VARCHAR(255),
    naics_industry_code VARCHAR(255),
    naics_industry_title VARCHAR(255),
    national_industry_code VARCHAR(255),
    national_industry_title VARCHAR(255),
    year_introduced INTEGER,
    year_updated INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    search_vector tsvector
);

-- Create the vector store table
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(768)
);

-- Create index on vector_store
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);

-- Create index on naics_codes search_vector
CREATE INDEX IF NOT EXISTS naics_codes_search_vector_idx ON naics_codes USING gin (search_vector);

-- Note: We've temporarily removed the search_vector column and related triggers
-- to simplify the initial test setup. These can be added back once the basic
-- repository tests are working.
