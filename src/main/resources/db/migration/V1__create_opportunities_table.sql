-- Create opportunities table if it doesn't exist
CREATE TABLE IF NOT EXISTS opportunities (
    notice_id VARCHAR(255) PRIMARY KEY,
    title TEXT,
    description TEXT,
    current_response_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    last_published_date TIMESTAMP,
    contract_opportunity_type VARCHAR(255),
    poc_information TEXT,
    active_inactive VARCHAR(50),
    awardee TEXT,
    contract_award_number VARCHAR(255),
    contract_award_date TIMESTAMP,
    naics TEXT,
    psc TEXT,
    modification_number VARCHAR(255),
    set_aside TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    vector_embedding VECTOR(768),
    search_vector TSVECTOR
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_opportunities_last_published_date ON opportunities(last_published_date);
CREATE INDEX IF NOT EXISTS idx_opportunities_naics ON opportunities(naics);
CREATE INDEX IF NOT EXISTS idx_opportunities_active_inactive ON opportunities(active_inactive);

-- Create function to generate search vectors
CREATE OR REPLACE FUNCTION opportunities_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop the trigger if it exists to avoid errors when rerunning
DROP TRIGGER IF EXISTS opportunities_search_vector_update_trigger ON opportunities;

-- Create trigger to automatically update search vectors
CREATE TRIGGER opportunities_search_vector_update_trigger
BEFORE INSERT OR UPDATE ON opportunities
FOR EACH ROW
EXECUTE FUNCTION opportunities_search_vector_update();

-- Create GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS idx_opportunities_search_vector ON opportunities USING GIN(search_vector);

-- Update existing records to populate the search_vector (if any exist)
UPDATE opportunities SET search_vector =
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B')
WHERE search_vector IS NULL;
