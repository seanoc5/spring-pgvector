-- Add vector embedding support to naics_codes table

-- Ensure pgvector extension is installed
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding_vector column to naics_codes table
ALTER TABLE naics_codes ADD COLUMN IF NOT EXISTS embedding_vector vector(768);

-- Create an index for similarity search
CREATE INDEX IF NOT EXISTS idx_naics_codes_embedding_vector ON naics_codes USING ivfflat (embedding_vector vector_cosine_ops) WITH (lists = 100);

-- Add a comment to explain the purpose of the column
COMMENT ON COLUMN naics_codes.embedding_vector IS 'Vector embedding for semantic search using pgvector. Generated from title and description.';
