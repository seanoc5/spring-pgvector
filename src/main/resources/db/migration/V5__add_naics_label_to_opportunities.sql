-- Add naicsLabel column to opportunities table if it doesn't exist
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS naics_label TEXT;

-- Update the search vector function to include naics_label in the search
CREATE OR REPLACE FUNCTION opportunities_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.naics, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.naics_label, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Update existing records to populate the search_vector with the new column
UPDATE opportunities SET search_vector =
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(naics, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(naics_label, '')), 'C')
WHERE search_vector IS NULL OR naics_label IS NOT NULL;
