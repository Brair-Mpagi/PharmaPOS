-- =========================================================================
-- PharmaPOS Sales Module - Initial Schema
-- Module prefix: sal_
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. Shifts (Cashier Sessions)
-- -------------------------------------------------------------------------
CREATE TABLE sal_shift (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    branch_id               UUID NOT NULL,
    device_id               UUID NOT NULL,
    user_id                 UUID NOT NULL,
    shift_number            VARCHAR(50) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    opened_by               UUID NOT NULL,
    closed_at               TIMESTAMP WITH TIME ZONE,
    closed_by               UUID,
    opening_cash_minor      BIGINT NOT NULL,
    closing_cash_minor      BIGINT,
    expected_cash_minor     BIGINT,
    variance_minor          BIGINT,
    currency                CHAR(3) NOT NULL DEFAULT 'UGX',
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_at              TIMESTAMP WITH TIME ZONE,
    updated_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT sal_shift_status_check CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT sal_shift_number_unique UNIQUE (tenant_id, branch_id, shift_number)
);

CREATE INDEX idx_sal_shift_tenant_branch ON sal_shift(tenant_id, branch_id);
CREATE INDEX idx_sal_shift_device ON sal_shift(device_id);
CREATE INDEX idx_sal_shift_user ON sal_shift(user_id);
CREATE INDEX idx_sal_shift_status ON sal_shift(tenant_id, branch_id, status);
CREATE INDEX idx_sal_shift_opened ON sal_shift(opened_at DESC);

COMMENT ON TABLE sal_shift IS 'Cashier shifts for cash reconciliation';

-- Row-Level Security
ALTER TABLE sal_shift ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_shift FORCE ROW LEVEL SECURITY;

CREATE POLICY sal_shift_tenant_isolation ON sal_shift
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 2. Sales
-- -------------------------------------------------------------------------
CREATE TABLE sal_sale (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    branch_id               UUID NOT NULL,
    shift_id                UUID REFERENCES sal_shift(id),
    device_id               UUID NOT NULL,
    receipt_number          VARCHAR(50) NOT NULL,
    idempotency_key         UUID NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_minor             BIGINT NOT NULL DEFAULT 0,
    tax_minor               BIGINT NOT NULL DEFAULT 0,
    discount_minor          BIGINT NOT NULL DEFAULT 0,
    currency                CHAR(3) NOT NULL DEFAULT 'UGX',
    customer_id             UUID,
    prescription_id         UUID,
    completed_at            TIMESTAMP WITH TIME ZONE,
    voided_at               TIMESTAMP WITH TIME ZONE,
    voided_by               UUID,
    void_reason             VARCHAR(255),
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_at              TIMESTAMP WITH TIME ZONE,
    updated_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT sal_sale_status_check 
        CHECK (status IN ('DRAFT', 'SUSPENDED', 'COMPLETED', 'VOIDED')),
    CONSTRAINT sal_sale_idempotency_unique UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT sal_sale_receipt_unique UNIQUE (tenant_id, branch_id, receipt_number)
);

CREATE INDEX idx_sal_sale_tenant_branch ON sal_sale(tenant_id, branch_id);
CREATE INDEX idx_sal_sale_shift ON sal_sale(shift_id);
CREATE INDEX idx_sal_sale_device ON sal_sale(device_id);
CREATE INDEX idx_sal_sale_customer ON sal_sale(customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_sal_sale_prescription ON sal_sale(prescription_id) WHERE prescription_id IS NOT NULL;
CREATE INDEX idx_sal_sale_status ON sal_sale(tenant_id, branch_id, status);
CREATE INDEX idx_sal_sale_completed ON sal_sale(completed_at DESC) WHERE completed_at IS NOT NULL;

COMMENT ON TABLE sal_sale IS 'Point of sale transactions';
COMMENT ON COLUMN sal_sale.idempotency_key IS 'UUIDv7 from terminal for idempotent finalization';

-- Row-Level Security
ALTER TABLE sal_sale ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_sale FORCE ROW LEVEL SECURITY;

CREATE POLICY sal_sale_tenant_isolation ON sal_sale
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 3. Sale Lines
-- -------------------------------------------------------------------------
CREATE TABLE sal_sale_line (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    sale_id                 UUID NOT NULL REFERENCES sal_sale(id) ON DELETE CASCADE,
    line_number             INTEGER NOT NULL,
    sellable_item_id        UUID NOT NULL,
    batch_id                UUID NOT NULL,
    unit                    VARCHAR(10) NOT NULL,
    quantity_value          NUMERIC(14,3) NOT NULL,
    unit_price_minor        BIGINT NOT NULL,
    unit_cost_minor         BIGINT NOT NULL,
    line_total_minor        BIGINT NOT NULL,
    tax_class_code          VARCHAR(50),
    tax_rate_percent        NUMERIC(5,2),
    tax_amount_minor        BIGINT NOT NULL DEFAULT 0,
    line_discount_minor     BIGINT NOT NULL DEFAULT 0,
    currency                CHAR(3) NOT NULL DEFAULT 'UGX',
    override_reason         VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    
    CONSTRAINT sal_sale_line_unique UNIQUE (tenant_id, sale_id, line_number)
);

CREATE INDEX idx_sal_sale_line_tenant ON sal_sale_line(tenant_id);
CREATE INDEX idx_sal_sale_line_sale ON sal_sale_line(sale_id);
CREATE INDEX idx_sal_sale_line_item ON sal_sale_line(sellable_item_id);
CREATE INDEX idx_sal_sale_line_batch ON sal_sale_line(batch_id);

COMMENT ON TABLE sal_sale_line IS 'Individual items in a sale with FEFO batch allocation';

-- Row-Level Security
ALTER TABLE sal_sale_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_sale_line FORCE ROW LEVEL SECURITY;

CREATE POLICY sal_sale_line_tenant_isolation ON sal_sale_line
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 4. Payments
-- -------------------------------------------------------------------------
CREATE TABLE sal_payment (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    sale_id                 UUID NOT NULL REFERENCES sal_sale(id) ON DELETE CASCADE,
    payment_number          INTEGER NOT NULL,
    method                  VARCHAR(20) NOT NULL,
    amount_minor            BIGINT NOT NULL,
    currency                CHAR(3) NOT NULL DEFAULT 'UGX',
    state                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_reference      VARCHAR(255),
    confirmed_at            TIMESTAMP WITH TIME ZONE,
    reversed_at             TIMESTAMP WITH TIME ZONE,
    reversal_reason         VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    
    CONSTRAINT sal_payment_method_check 
        CHECK (method IN ('CASH', 'MOBILE_MONEY', 'CARD', 'CREDIT', 'BANK_TRANSFER')),
    CONSTRAINT sal_payment_state_check 
        CHECK (state IN ('PENDING', 'CONFIRMED', 'FAILED', 'REVERSED')),
    CONSTRAINT sal_payment_unique UNIQUE (tenant_id, sale_id, payment_number)
);

CREATE INDEX idx_sal_payment_tenant ON sal_payment(tenant_id);
CREATE INDEX idx_sal_payment_sale ON sal_payment(sale_id);
CREATE INDEX idx_sal_payment_state ON sal_payment(tenant_id, state);
CREATE INDEX idx_sal_payment_provider ON sal_payment(provider_reference) WHERE provider_reference IS NOT NULL;

COMMENT ON TABLE sal_payment IS 'Payments for sales - multiple payments per sale supported';

-- Row-Level Security
ALTER TABLE sal_payment ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_payment FORCE ROW LEVEL SECURITY;

CREATE POLICY sal_payment_tenant_isolation ON sal_payment
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 5. Cash Movements (Shift-related)
-- -------------------------------------------------------------------------
CREATE TABLE sal_cash_movement (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    branch_id               UUID NOT NULL,
    shift_id                UUID NOT NULL REFERENCES sal_shift(id),
    movement_type           VARCHAR(20) NOT NULL,
    amount_minor            BIGINT NOT NULL,
    currency                CHAR(3) NOT NULL DEFAULT 'UGX',
    reason                  VARCHAR(255),
    notes                   TEXT,
    occurred_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              UUID,
    
    CONSTRAINT sal_cash_movement_type_check 
        CHECK (movement_type IN ('CASH_IN', 'CASH_OUT', 'PETTY_CASH', 'BANK_DEPOSIT'))
);

CREATE INDEX idx_sal_cash_movement_tenant_branch ON sal_cash_movement(tenant_id, branch_id);
CREATE INDEX idx_sal_cash_movement_shift ON sal_cash_movement(shift_id);
CREATE INDEX idx_sal_cash_movement_occurred ON sal_cash_movement(occurred_at DESC);

COMMENT ON TABLE sal_cash_movement IS 'Non-sale cash movements (petty cash, bank deposits, etc.)';

-- Row-Level Security
ALTER TABLE sal_cash_movement ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_cash_movement FORCE ROW LEVEL SECURITY;

CREATE POLICY sal_cash_movement_tenant_isolation ON sal_cash_movement
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- =========================================================================
-- Functions
-- =========================================================================

-- Generate sequential receipt number
CREATE OR REPLACE FUNCTION sal_generate_receipt_number(
    p_tenant_id UUID,
    p_branch_id UUID
) RETURNS VARCHAR AS $$
DECLARE
    v_date_prefix VARCHAR(8);
    v_sequence INTEGER;
    v_receipt_number VARCHAR(50);
BEGIN
    -- Format: YYYYMMDD-NNNN
    v_date_prefix := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    
    -- Get next sequence for today
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(receipt_number FROM 10) AS INTEGER)
    ), 0) + 1
    INTO v_sequence
    FROM sal_sale
    WHERE tenant_id = p_tenant_id
      AND branch_id = p_branch_id
      AND receipt_number LIKE v_date_prefix || '-%';
    
    v_receipt_number := v_date_prefix || '-' || LPAD(v_sequence::TEXT, 4, '0');
    
    RETURN v_receipt_number;
END;
$$ LANGUAGE plpgsql;

COMMENT ON SCHEMA public IS 'PharmaPOS Sales Schema - V001 Initial';
