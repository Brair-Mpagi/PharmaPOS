-- =========================================================================
-- PharmaPOS Catalogue Module - Initial Schema
-- Module prefix: cat_
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. Sellable Items (Products that can be sold)
-- -------------------------------------------------------------------------
CREATE TABLE cat_sellable_item (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    generic_name            VARCHAR(255),
    active_ingredient       VARCHAR(500),
    strength                VARCHAR(100),
    dosage_form             VARCHAR(50),
    base_unit               VARCHAR(10) NOT NULL,
    pack_size               NUMERIC(10,2),
    manufacturer            VARCHAR(255),
    requires_prescription   BOOLEAN NOT NULL DEFAULT FALSE,
    is_controlled           BOOLEAN NOT NULL DEFAULT FALSE,
    schedule                VARCHAR(20),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description             TEXT,
    storage_conditions      VARCHAR(255),
    tax_class_id            UUID,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_at              TIMESTAMP WITH TIME ZONE,
    updated_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT cat_sellable_item_status_check 
        CHECK (status IN ('ACTIVE', 'DISCONTINUED', 'SUSPENDED')),
    CONSTRAINT cat_sellable_item_schedule_check 
        CHECK (schedule IS NULL OR schedule IN ('II', 'III', 'IV', 'V'))
);

CREATE INDEX idx_cat_sellable_item_tenant ON cat_sellable_item(tenant_id);
CREATE INDEX idx_cat_sellable_item_name ON cat_sellable_item(tenant_id, name);
CREATE INDEX idx_cat_sellable_item_status ON cat_sellable_item(tenant_id, status);
CREATE INDEX idx_cat_sellable_item_search ON cat_sellable_item USING gin(to_tsvector('english', name || ' ' || COALESCE(generic_name, '') || ' ' || COALESCE(active_ingredient, '')));
CREATE INDEX idx_cat_sellable_item_name_trgm ON cat_sellable_item USING gin(name gin_trgm_ops);

COMMENT ON TABLE cat_sellable_item IS 'Products that can be sold, combining product definition and SKU';
COMMENT ON COLUMN cat_sellable_item.schedule IS 'DEA schedule for controlled substances';

-- Row-Level Security
ALTER TABLE cat_sellable_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE cat_sellable_item FORCE ROW LEVEL SECURITY;

CREATE POLICY cat_sellable_item_tenant_isolation ON cat_sellable_item
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 2. Barcodes
-- -------------------------------------------------------------------------
CREATE TABLE cat_barcode (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    sellable_item_id    UUID NOT NULL REFERENCES cat_sellable_item(id) ON DELETE CASCADE,
    code                VARCHAR(100) NOT NULL,
    type                VARCHAR(20) NOT NULL,
    is_primary          BOOLEAN NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT cat_barcode_type_check 
        CHECK (type IN ('GTIN', 'EAN13', 'EAN8', 'UPC', 'CODE128', 'INTERNAL')),
    CONSTRAINT cat_barcode_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE INDEX idx_cat_barcode_tenant ON cat_barcode(tenant_id);
CREATE INDEX idx_cat_barcode_code ON cat_barcode(tenant_id, code) WHERE is_active = TRUE;
CREATE INDEX idx_cat_barcode_item ON cat_barcode(sellable_item_id);

COMMENT ON TABLE cat_barcode IS 'Multiple barcodes per item for scanning flexibility';

-- Row-Level Security
ALTER TABLE cat_barcode ENABLE ROW LEVEL SECURITY;
ALTER TABLE cat_barcode FORCE ROW LEVEL SECURITY;

CREATE POLICY cat_barcode_tenant_isolation ON cat_barcode
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 3. Tax Classes
-- -------------------------------------------------------------------------
CREATE TABLE cat_tax_class (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    code                VARCHAR(50) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    rate_percent        NUMERIC(5,2) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    description         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_at          TIMESTAMP WITH TIME ZONE,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT cat_tax_class_rate_check CHECK (rate_percent >= 0 AND rate_percent <= 100),
    CONSTRAINT cat_tax_class_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE INDEX idx_cat_tax_class_tenant ON cat_tax_class(tenant_id);
CREATE INDEX idx_cat_tax_class_active ON cat_tax_class(tenant_id, is_active);

COMMENT ON TABLE cat_tax_class IS 'Tax rates applicable to items (VAT, GST, etc.)';

-- Row-Level Security
ALTER TABLE cat_tax_class ENABLE ROW LEVEL SECURITY;
ALTER TABLE cat_tax_class FORCE ROW LEVEL SECURITY;

CREATE POLICY cat_tax_class_tenant_isolation ON cat_tax_class
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 4. Price Lists
-- -------------------------------------------------------------------------
CREATE TABLE cat_price_list (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    is_default          BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from          DATE NOT NULL,
    valid_to            DATE,
    currency            CHAR(3) NOT NULL DEFAULT 'UGX',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_at          TIMESTAMP WITH TIME ZONE,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT cat_price_list_dates_check CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT cat_price_list_tenant_name_unique UNIQUE (tenant_id, name)
);

CREATE INDEX idx_cat_price_list_tenant ON cat_price_list(tenant_id);
CREATE INDEX idx_cat_price_list_default ON cat_price_list(tenant_id, is_default) WHERE is_default = TRUE;
CREATE INDEX idx_cat_price_list_valid ON cat_price_list(tenant_id, valid_from, valid_to) WHERE is_active = TRUE;

COMMENT ON TABLE cat_price_list IS 'Named price lists for different pricing strategies';

-- Row-Level Security
ALTER TABLE cat_price_list ENABLE ROW LEVEL SECURITY;
ALTER TABLE cat_price_list FORCE ROW LEVEL SECURITY;

CREATE POLICY cat_price_list_tenant_isolation ON cat_price_list
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 5. Price List Items
-- -------------------------------------------------------------------------
CREATE TABLE cat_price_list_item (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    price_list_id       UUID NOT NULL REFERENCES cat_price_list(id) ON DELETE CASCADE,
    sellable_item_id    UUID NOT NULL REFERENCES cat_sellable_item(id) ON DELETE CASCADE,
    unit_price_minor    BIGINT NOT NULL,
    currency            CHAR(3) NOT NULL DEFAULT 'UGX',
    min_quantity        NUMERIC(10,2),
    max_quantity        NUMERIC(10,2),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_at          TIMESTAMP WITH TIME ZONE,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT cat_price_list_item_price_check CHECK (unit_price_minor >= 0),
    CONSTRAINT cat_price_list_item_quantity_check 
        CHECK (min_quantity IS NULL OR max_quantity IS NULL OR max_quantity >= min_quantity),
    CONSTRAINT cat_price_list_item_unique UNIQUE (tenant_id, price_list_id, sellable_item_id, min_quantity)
);

CREATE INDEX idx_cat_price_list_item_tenant ON cat_price_list_item(tenant_id);
CREATE INDEX idx_cat_price_list_item_price_list ON cat_price_list_item(price_list_id);
CREATE INDEX idx_cat_price_list_item_item ON cat_price_list_item(sellable_item_id);

COMMENT ON TABLE cat_price_list_item IS 'Individual item prices within a price list, supports quantity breaks';

-- Row-Level Security
ALTER TABLE cat_price_list_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE cat_price_list_item FORCE ROW LEVEL SECURITY;

CREATE POLICY cat_price_list_item_tenant_isolation ON cat_price_list_item
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- =========================================================================
-- Seed Standard Tax Classes
-- =========================================================================

-- Note: Actual seeding will be done via application code or data migration
-- This is a placeholder showing the structure

COMMENT ON SCHEMA public IS 'PharmaPOS Catalogue Schema - V001 Initial';
