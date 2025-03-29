-- Improve full-text search for NAICS codes

-- Drop existing trigger to recreate with improved version
DROP TRIGGER IF EXISTS naics_codes_search_vector_update_trigger ON naics_codes;

-- Create improved function to generate search vectors for naics_codes
CREATE OR REPLACE FUNCTION naics_codes_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    -- Normalize text before indexing to improve search quality
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

-- Create trigger to automatically update search vectors
CREATE TRIGGER naics_codes_search_vector_update_trigger
BEFORE INSERT OR UPDATE ON naics_codes
FOR EACH ROW
EXECUTE FUNCTION naics_codes_search_vector_update();

-- Update existing records to populate search_vector with improved indexing
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

-- Create additional lexeme dictionary entries for common agricultural terms
-- This helps with domain-specific terms that might not be in the standard English dictionary
DO $$
BEGIN
    -- Only run if the pg_dict_simple extension is available
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_dict_simple') THEN
        -- Add common agricultural terms to the dictionary
        INSERT INTO pg_ts_dict (dict_name, dict_init, dict_lexize, dict_options)
        VALUES ('english_agriculture', 'simple_init', 'simple_lexize',
                '{"soybean", "soybeans", "oilseed", "oilseeds", "farming", "agriculture", "crop", "crops"}')
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
