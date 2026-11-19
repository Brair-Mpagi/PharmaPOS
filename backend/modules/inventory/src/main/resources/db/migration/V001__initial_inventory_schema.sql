-- =========================================================================
-- PharmaPOS Inventory Module - Initial Schema
-- Module prefix: inv_
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. Stock Batches
-- -------------------------------------------------------------------------
CREATE TABLE inv_stock_batch (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    branch_id               UUID NOT NULL,
    sellable_item_id        UUID NOT NULL,
    batch_number            VARCHAR(100),
    expiry_date             DATE,
    supplier_id             UUID,
    grn_line_id             UUID,
    unit_cost_minor         BIGINT NOT NULL,
    currency                CHAR(3) NOT NULL DEFAULT 'UGX',
    status                  VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    quarantine_reason       VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_at              TIMESTAMP WITH TIME ZONE,
    updated_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT inv_stock_batch_status_check 
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'QUARANTINED', 'EXPIRED', 'RECALLED', 'DAMAGED')),
    CONSTRAINT inv_stock_batch_unique 
        UNIQUE (tenant_id, branch_id, sellable_item_id, batch_number, supplier_id)
);

CREATE INDEX idx_inv_stock_batch_tenant_branch ON inv_stock_batch(tenant_id, branch_id);
CREATE INDEX idx_inv_stock_batch_item ON inv_stock_batch(sellable_item_id);
CREATE INDEX idx_inv_stock_batch_status ON inv_stock_batch(tenant_id, branch_id, status);
CREATE INDEX idx_inv_stock_batch_fefo ON inv_stock_batch(tenant_id, branch_id, sellable_item_id, status, expiry_date)
    WHERE status = 'AVAILABLE';

COMMENT ON TABLE inv_stock_batch IS 'Physical stock batches with expiry tracking';
COMMENT ON INDEX idx_inv_stock_batch_fefo IS 'Optimized for FEFO (First Expired, First Out) allocation';

-- Row-Level Security
ALTER TABLE inv_stock_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE inv_stock_batch FORCE ROW LEVEL SECURITY;

CREATE POLICY inv_stock_batch_tenant_isolation ON inv_stock_batch
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 2. Stock Movements (Immutable Event Log)
-- -------------------------------------------------------------------------
CREATE TABLE inv_stock_movement (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    branch_id               UUID NOT NULL,
    batch_id                UUID NOT NULL REFERENCES inv_stock_batch(id),
    movement_type           VARCHAR(30) NOT NULL,
    quantity_value          NUMERIC(14,3) NOT NULL,
    quantity_unit           VARCHAR(10) NOT NULL,
    source_doc_type         VARCHAR(50),
    source_doc_id           UUID,
    reason_code             VARCHAR(50),
    note                    TEXT,
    occurred_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    origin_device_id        UUID,
    origin_seq              BIGINT,
    created_by              UUID,
    
    CONSTRAINT inv_stock_movement_type_check 
        CHECK (movement_type IN ('RECEIPT', 'SALE', 'RETURN', 'TRANSFER_OUT', 'TRANSFER_IN', 
                                  'ADJUSTMENT', 'DAMAGE', 'LOSS', 'EXPIRY_WRITE_OFF', 
                                  'QUARANTINE', 'RELEASE'))
);

CREATE INDEX idx_inv_stock_movement_tenant_branch ON inv_stock_movement(tenant_id, branch_id);
CREATE INDEX idx_inv_stock_movement_batch ON inv_stock_movement(batch_id);
CREATE INDEX idx_inv_stock_movement_occurred ON inv_stock_movement(occurred_at DESC);
CREATE INDEX idx_inv_stock_movement_source ON inv_stock_movement(source_doc_type, source_doc_id);

COMMENT ON TABLE inv_stock_movement IS 'Immutable audit log of all stock movements';
COMMENT ON COLUMN inv_stock_movement.occurred_at IS 'When movement happened (device time)';
COMMENT ON COLUMN inv_stock_movement.recorded_at IS 'When movement was recorded (server time)';

-- Row-Level Security
ALTER TABLE inv_stock_movement ENABLE ROW LEVEL SECURITY;
ALTER TABLE inv_stock_movement FORCE ROW LEVEL SECURITY;

CREATE POLICY inv_stock_movement_tenant_isolation ON inv_stock_movement
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 3. Stock Balances (Computed View)
-- -------------------------------------------------------------------------
CREATE TABLE inv_stock_balance (
    id                          UUID PRIMARY KEY,
    tenant_id                   UUID NOT NULL,
    branch_id                   UUID NOT NULL,
    batch_id                    UUID NOT NULL REFERENCES inv_stock_batch(id) ON DELETE CASCADE,
    quantity_on_hand_value      NUMERIC(14,3) NOT NULL DEFAULT 0,
    quantity_on_hand_unit       VARCHAR(10) NOT NULL,
    quantity_reserved_value     NUMERIC(14,3) NOT NULL DEFAULT 0,
    quantity_reserved_unit      VARCHAR(10) NOT NULL,
    last_movement_at            TIMESTAMP WITH TIME ZONE,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version                     BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT inv_stock_balance_batch_unique UNIQUE (tenant_id, branch_id, batch_id)
);

CREATE INDEX idx_inv_stock_balance_tenant_branch ON inv_stock_balance(tenant_id, branch_id);
CREATE INDEX idx_inv_stock_balance_batch ON inv_stock_balance(batch_id);

COMMENT ON TABLE inv_stock_balance IS 'Current stock levels - projection from movements';

-- Row-Level Security
ALTER TABLE inv_stock_balance ENABLE ROW LEVEL SECURITY;
ALTER TABLE inv_stock_balance FORCE ROW LEVEL SECURITY;

CREATE POLICY inv_stock_balance_tenant_isolation ON inv_stock_balance
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 4. Stock Counts
-- -------------------------------------------------------------------------
CREATE TABLE inv_stock_count (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    branch_id               UUID NOT NULL,
    count_number            VARCHAR(50) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_by              UUID NOT NULL,
    completed_at            TIMESTAMP WITH TIME ZONE,
    completed_by            UUID,
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_at              TIMESTAMP WITH TIME ZONE,
    updated_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT inv_stock_count_status_check 
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT inv_stock_count_number_unique UNIQUE (tenant_id, branch_id, count_number)
);

CREATE INDEX idx_inv_stock_count_tenant_branch ON inv_stock_count(tenant_id, branch_id);
CREATE INDEX idx_inv_stock_count_status ON inv_stock_count(tenant_id, branch_id, status);

COMMENT ON TABLE inv_stock_count IS 'Physical stock count sessions';

-- Row-Level Security
ALTER TABLE inv_stock_count ENABLE ROW LEVEL SECURITY;
ALTER TABLE inv_stock_count FORCE ROW LEVEL SECURITY;

CREATE POLICY inv_stock_count_tenant_isolation ON inv_stock_count
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 5. Stock Count Lines
-- -------------------------------------------------------------------------
CREATE TABLE inv_stock_count_line (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    count_id                UUID NOT NULL REFERENCES inv_stock_count(id) ON DELETE CASCADE,
    batch_id                UUID NOT NULL REFERENCES inv_stock_batch(id),
    expected_quantity_value NUMERIC(14,3),
    expected_quantity_unit  VARCHAR(10),
    counted_quantity_value  NUMERIC(14,3) NOT NULL,
    counted_quantity_unit   VARCHAR(10) NOT NULL,
    variance_value          NUMERIC(14,3),
    variance_unit           VARCHAR(10),
    counted_by              UUID,
    counted_at              TIMESTAMP WITH TIME ZONE,
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_inv_stock_count_line_count ON inv_stock_count_line(count_id);
CREATE INDEX idx_inv_stock_count_line_batch ON inv_stock_count_line(batch_id);

COMMENT ON TABLE inv_stock_count_line IS 'Individual batch counts within a count session';

-- Row-Level Security
ALTER TABLE inv_stock_count_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE inv_stock_count_line FORCE ROW LEVEL SECURITY;

CREATE POLICY inv_stock_count_line_tenant_isolation ON inv_stock_count_line
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- =========================================================================
-- Functions and Triggers
-- =========================================================================

-- Function to update stock balance after movement
CREATE OR REPLACE FUNCTION inv_update_balance_after_movement()
RETURNS TRIGGER AS $$
BEGIN
    -- Update or insert balance
    INSERT INTO inv_stock_balance (
        id, tenant_id, branch_id, batch_id,
        quantity_on_hand_value, quantity_on_hand_unit,
        quantity_reserved_value, quantity_reserved_unit,
        last_movement_at
    )
    SELECT
        gen_random_uuid(),
        NEW.tenant_id,
        NEW.branch_id,
        NEW.batch_id,
        NEW.quantity_value,
        NEW.quantity_unit,
        0,
        NEW.quantity_unit,
        NEW.occurred_at
    ON CONFLICT (tenant_id, branch_id, batch_id)
    DO UPDATE SET
        quantity_on_hand_value = inv_stock_balance.quantity_on_hand_value + EXCLUDED.quantity_on_hand_value,
        last_movement_at = EXCLUDED.last_movement_at,
        updated_at = NOW();
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update balances
CREATE TRIGGER trg_inv_update_balance_after_movement
    AFTER INSERT ON inv_stock_movement
    FOR EACH ROW
    EXECUTE FUNCTION inv_update_balance_after_movement();

COMMENT ON SCHEMA public IS 'PharmaPOS Inventory Schema - V001 Initial';
