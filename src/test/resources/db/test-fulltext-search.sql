-- SQL script to test PostgreSQL full-text search functionality
-- This can be run directly against the database to diagnose issues

-- First, let's examine the structure of the search_vector column
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'naics_codes' AND column_name = 'search_vector';

-- Check if the trigger exists
SELECT trigger_name, event_manipulation, action_statement
FROM information_schema.triggers
WHERE event_object_table = 'naics_codes';

-- Insert a test record with "soybean" in the title and description
INSERT INTO naics_codes (code, title, description, level, is_active)
VALUES ('TEST01', 'Test Soybean Entry', 'This is a test entry about soybeans.', 1, true);

-- Check if the search_vector was populated correctly
SELECT code, title, search_vector
FROM naics_codes
WHERE code = 'TEST01';

-- Test a direct search using to_tsquery
SELECT code, title, description, ts_rank(search_vector, to_tsquery('english', 'soybean:*')) as rank
FROM naics_codes
WHERE search_vector @@ to_tsquery('english', 'soybean:*')
ORDER BY rank DESC;

-- Test with stemming to see if "soybeans" matches "soybean"
SELECT code, title, description, ts_rank(search_vector, to_tsquery('english', 'soybeans:*')) as rank
FROM naics_codes
WHERE search_vector @@ to_tsquery('english', 'soybeans:*')
ORDER BY rank DESC;

-- Test with different word forms
SELECT word, dictionary
FROM ts_debug('english', 'soybeans');

SELECT word, dictionary
FROM ts_debug('english', 'soybean');

-- Check if the search is working with multiple terms
SELECT code, title, description, ts_rank(search_vector, to_tsquery('english', 'soybean:* & farming:*')) as rank
FROM naics_codes
WHERE search_vector @@ to_tsquery('english', 'soybean:* & farming:*')
ORDER BY rank DESC;

-- Test with OR operator instead of AND
SELECT code, title, description, ts_rank(search_vector, to_tsquery('english', 'soybean:* | farming:*')) as rank
FROM naics_codes
WHERE search_vector @@ to_tsquery('english', 'soybean:* | farming:*')
ORDER BY rank DESC;

-- Clean up the test record
DELETE FROM naics_codes WHERE code = 'TEST01';
