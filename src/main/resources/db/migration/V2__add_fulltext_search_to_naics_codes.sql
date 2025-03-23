-- Add search_vector column to naics_codes table if it doesn't exist
ALTER TABLE naics_codes ADD COLUMN IF NOT EXISTS search_vector TSVECTOR;

-- Create function to generate search vectors for naics_codes
CREATE OR REPLACE FUNCTION naics_codes_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector = 
        setweight(to_tsvector('english', COALESCE(NEW.code, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.sector_title, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.subsector_title, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.industry_group_title, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.naics_industry_title, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.national_industry_title, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop the trigger if it exists to avoid errors when rerunning
DROP TRIGGER IF EXISTS naics_codes_search_vector_update_trigger ON naics_codes;

-- Create trigger to automatically update search vectors
CREATE TRIGGER naics_codes_search_vector_update_trigger
BEFORE INSERT OR UPDATE ON naics_codes
FOR EACH ROW
EXECUTE FUNCTION naics_codes_search_vector_update();

-- Create GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS idx_naics_codes_search_vector ON naics_codes USING GIN(search_vector);

-- Update existing records to populate search_vector
UPDATE naics_codes
SET search_vector = 
    setweight(to_tsvector('english', COALESCE(code, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(sector_title, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(subsector_title, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(industry_group_title, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(naics_industry_title, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(national_industry_title, '')), 'C');
