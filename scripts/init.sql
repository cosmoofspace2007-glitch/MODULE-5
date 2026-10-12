-- 1. Bảng tài khoản (Accounts)
CREATE TABLE IF NOT EXISTS accounts (
                                        id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    account_type VARCHAR(20) NOT NULL, -- 'USER_WALLET', 'SYSTEM_CLEARING', 'SYSTEM_FEE'
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    balance NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );
-- 2. Bảng giao dịch tổng thể (Transactions)
CREATE TABLE IF NOT EXISTS transactions (
                                            transaction_id VARCHAR(64) PRIMARY KEY,
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    tx_type VARCHAR(30) NOT NULL, -- 'TRANSFER', 'TOPUP', 'WITHDRAW', 'PAYMENT'
    source_account_id VARCHAR(64) NOT NULL,
    destination_account_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'SUCCESS', 'FAILED'
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );
-- 3. Bảng Sổ cái Kế toán Kép (Ledger Entries) - BẤT BIẾN (IMMUTABLE / APPEND-ONLY)
CREATE TABLE IF NOT EXISTS ledger_entries (
                                              entry_id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL REFERENCES transactions(transaction_id),
    account_id VARCHAR(64) NOT NULL REFERENCES accounts(id),
    direction VARCHAR(10) NOT NULL, -- 'DEBIT' hoặc 'CREDIT'
    amount NUMERIC(18, 2) NOT NULL, -- Luôn là số tiền dương
    balance_after NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );
CREATE INDEX IF NOT EXISTS idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_tx_id ON ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_tx_idempotency ON transactions(idempotency_key);
-- ====================================================================
-- KHỞI TẠO DỮ LIỆU BAN ĐẦU (SEED DATA)
-- ====================================================================
-- Tài khoản trung gian hệ thống (System Master / Clearing Account)
-- Dùng để đối ứng kế toán kép khi nạp/rút tiền qua Ngân hàng
INSERT INTO accounts (id, user_id, account_type, currency, balance, version, created_at, updated_at)
VALUES
    ('ACC_SYSTEM_CLEARING', 'SYSTEM', 'SYSTEM_CLEARING', 'VND', 1000000000.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO NOTHING;
-- Tài khoản Người dùng thử nghiệm 1
INSERT INTO accounts (id, user_id, account_type, currency, balance, version, created_at, updated_at)
VALUES
    ('ACC_USER_01', 'USER_001', 'USER_WALLET', 'VND', 5000000.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO NOTHING;
-- Tài khoản Người dùng thử nghiệm 2
INSERT INTO accounts (id, user_id, account_type, currency, balance, version, created_at, updated_at)
VALUES
    ('ACC_USER_02', 'USER_002', 'USER_WALLET', 'VND', 2000000.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO NOTHING;

ALTER TABLE accounts ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';