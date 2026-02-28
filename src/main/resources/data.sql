-- ============================================================
-- SAMPLE DATA - Risk Operations Dashboard
-- ============================================================
-- These scripts populate the H2 in-memory DB on startup.
-- For PostgreSQL: run manually after schema creation.
-- ============================================================

-- NOTE: With JPA ddl-auto=create-drop, Hibernate creates tables.
-- This file seeds sample merchants + transactions for demo/testing.

-- ============================================================
-- MERCHANTS (representing a range of risk profiles)
-- ============================================================

INSERT INTO merchants (id, name, email, business_type, risk_status, risk_score, review_pending, flagged_at, resolved_at, created_at, updated_at)
VALUES
  -- Low risk: established business, clean history
  ('a1b2c3d4-0001-0001-0001-000000000001',
   'GreenLeaf Payments', 'ops@greenleaf.io', 'E_COMMERCE',
   'LOW', 5, false, NULL, NULL, DATEADD('DAY', -90, NOW()), NOW()),

  -- Low risk: new merchant, minimal activity
  ('a1b2c3d4-0002-0002-0002-000000000002',
   'Sunrise Tech Ltd', 'billing@sunrisetech.com', 'SAAS',
   'LOW', 12, false, NULL, NULL, DATEADD('DAY', -30, NOW()), NOW()),

  -- Medium risk: elevated transaction frequency
  ('a1b2c3d4-0003-0003-0003-000000000003',
   'FastCash Express', 'risk@fastcash.io', 'MONEY_TRANSFER',
   'MEDIUM', 52, true, DATEADD('DAY', -2, NOW()), NULL, DATEADD('DAY', -60, NOW()), NOW()),

  -- Medium risk: large transaction amounts
  ('a1b2c3d4-0004-0004-0004-000000000004',
   'Meridian Trading Co', 'compliance@meridian.co', 'TRADING',
   'MEDIUM', 48, false, NULL, NULL, DATEADD('DAY', -45, NOW()), NOW()),

  -- High risk: multiple rule triggers
  ('a1b2c3d4-0005-0005-0005-000000000005',
   'VelocityPay Corp', 'admin@velocitypay.io', 'PAYMENTS',
   'HIGH', 78, true, DATEADD('DAY', -1, NOW()), NULL, DATEADD('DAY', -20, NOW()), NOW()),

  -- High risk: flagged for AML review
  ('a1b2c3d4-0006-0006-0006-000000000006',
   'Global Remit Hub', 'ops@globalremit.net', 'REMITTANCE',
   'HIGH', 82, true, NOW(), NULL, DATEADD('DAY', -15, NOW()), NOW()),

  -- Frozen: auto-frozen by risk engine
  ('a1b2c3d4-0007-0007-0007-000000000007',
   'Shadow Commerce LLC', 'contact@shadowcommerce.io', 'E_COMMERCE',
   'FROZEN', 94, true, DATEADD('DAY', -3, NOW()), NULL, DATEADD('DAY', -10, NOW()), NOW()),

  -- Frozen: manually frozen by risk team
  ('a1b2c3d4-0008-0008-0008-000000000008',
   'QuickDraw Finance', 'admin@quickdraw.finance', 'LENDING',
   'FROZEN', 91, false, DATEADD('DAY', -7, NOW()), NULL, DATEADD('DAY', -120, NOW()), NOW()),

  -- Low risk: recently resolved from HIGH
  ('a1b2c3d4-0009-0009-0009-000000000009',
   'ClearPath Merchants', 'payments@clearpath.io', 'RETAIL',
   'LOW', 18, false, DATEADD('DAY', -5, NOW()), NOW(), DATEADD('DAY', -75, NOW()), NOW()),

  -- Medium risk: in active review
  ('a1b2c3d4-0010-0010-0010-000000000010',
   'Horizon Finserv', 'ops@horizon.fin', 'FINANCIAL_SERVICES',
   'MEDIUM', 61, true, DATEADD('HOUR', -4, NOW()), NULL, DATEADD('DAY', -50, NOW()), NOW());


-- ============================================================
-- MERCHANT FEATURES
-- ============================================================

-- GreenLeaf: all features enabled
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0001-0001-0001-000000000001', 'PAYMENTS'),
  ('a1b2c3d4-0001-0001-0001-000000000001', 'WITHDRAWALS'),
  ('a1b2c3d4-0001-0001-0001-000000000001', 'API_ACCESS');

-- Sunrise Tech: all features
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0002-0002-0002-000000000002', 'PAYMENTS'),
  ('a1b2c3d4-0002-0002-0002-000000000002', 'WITHDRAWALS'),
  ('a1b2c3d4-0002-0002-0002-000000000002', 'API_ACCESS');

-- FastCash: withdrawals disabled (medium risk restriction)
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0003-0003-0003-000000000003', 'PAYMENTS'),
  ('a1b2c3d4-0003-0003-0003-000000000003', 'API_ACCESS');

-- Meridian: all features
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0004-0004-0004-000000000004', 'PAYMENTS'),
  ('a1b2c3d4-0004-0004-0004-000000000004', 'WITHDRAWALS'),
  ('a1b2c3d4-0004-0004-0004-000000000004', 'API_ACCESS');

-- VelocityPay: only payments (restricted due to HIGH risk)
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0005-0005-0005-000000000005', 'PAYMENTS');

-- Global Remit: payments only
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0006-0006-0006-000000000006', 'PAYMENTS');

-- Shadow Commerce: FROZEN - no features (frozen merchants have empty feature set)
-- QuickDraw: FROZEN - no features

-- ClearPath: all features restored after resolution
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0009-0009-0009-000000000009', 'PAYMENTS'),
  ('a1b2c3d4-0009-0009-0009-000000000009', 'WITHDRAWALS'),
  ('a1b2c3d4-0009-0009-0009-000000000009', 'API_ACCESS');

-- Horizon Finserv: payments only during review
INSERT INTO merchant_features (merchant_id, feature) VALUES
  ('a1b2c3d4-0010-0010-0010-000000000010', 'PAYMENTS');


-- ============================================================
-- SAMPLE TRANSACTIONS (drives risk engine evaluation)
-- ============================================================

-- GreenLeaf: normal activity
INSERT INTO transactions (id, merchant_id, amount, currency, transaction_type, flagged, created_at) VALUES
  (RANDOM_UUID(), 'a1b2c3d4-0001-0001-0001-000000000001', 245.00, 'USD', 'PAYMENT', false, DATEADD('HOUR', -2, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0001-0001-0001-000000000001', 1200.00, 'USD', 'PAYMENT', false, DATEADD('HOUR', -4, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0001-0001-0001-000000000001', 89.99, 'USD', 'PAYMENT', false, DATEADD('HOUR', -6, NOW()));

-- VelocityPay: high frequency (triggers velocity rule)
INSERT INTO transactions (id, merchant_id, amount, currency, transaction_type, flagged, created_at) VALUES
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', false, DATEADD('MINUTE', -10, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', false, DATEADD('MINUTE', -15, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', false, DATEADD('MINUTE', -20, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', false, DATEADD('MINUTE', -25, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', false, DATEADD('MINUTE', -30, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', true, DATEADD('MINUTE', -35, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', true, DATEADD('MINUTE', -40, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', true, DATEADD('MINUTE', -45, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', true, DATEADD('MINUTE', -50, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0005-0005-0005-000000000005', 500.00, 'USD', 'PAYMENT', true, DATEADD('MINUTE', -55, NOW()));

-- Global Remit: large amounts (triggers amount threshold rule)
INSERT INTO transactions (id, merchant_id, amount, currency, transaction_type, flagged, created_at) VALUES
  (RANDOM_UUID(), 'a1b2c3d4-0006-0006-0006-000000000006', 48000.00, 'USD', 'WITHDRAWAL', true, DATEADD('HOUR', -3, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0006-0006-0006-000000000006', 52000.00, 'USD', 'WITHDRAWAL', true, DATEADD('HOUR', -6, NOW())),
  (RANDOM_UUID(), 'a1b2c3d4-0006-0006-0006-000000000006', 35000.00, 'USD', 'PAYMENT', true, DATEADD('HOUR', -9, NOW()));


-- ============================================================
-- AUDIT LOG SEED (pre-existing history)
-- ============================================================

INSERT INTO audit_logs (id, action_type, entity_type, entity_id, old_value, new_value, performed_by, correlation_id, description, timestamp)
VALUES
  -- VelocityPay was manually escalated to HIGH
  (RANDOM_UUID(), 'RISK_STATUS_CHANGED', 'MERCHANT', 'a1b2c3d4-0005-0005-0005-000000000005',
   'MEDIUM', 'HIGH', 'riskops',
   NULL, 'Manual escalation after velocity spike detected', DATEADD('DAY', -1, NOW())),

  -- Shadow Commerce auto-frozen
  (RANDOM_UUID(), 'RISK_AUTO_FROZEN', 'MERCHANT', 'a1b2c3d4-0007-0007-0007-000000000007',
   'HIGH', 'FROZEN', 'RISK_ENGINE',
   NULL, 'Auto-frozen: score 94/100 exceeded threshold', DATEADD('DAY', -3, NOW())),

  -- QuickDraw manually frozen by admin
  (RANDOM_UUID(), 'MERCHANT_FROZEN', 'MERCHANT', 'a1b2c3d4-0008-0008-0008-000000000008',
   'HIGH', 'FROZEN', 'admin',
   NULL, 'Manual freeze: suspected structuring pattern', DATEADD('DAY', -7, NOW())),

  -- ClearPath resolved
  (RANDOM_UUID(), 'RISK_STATUS_CHANGED', 'MERCHANT', 'a1b2c3d4-0009-0009-0009-000000000009',
   'HIGH', 'LOW', 'riskops',
   NULL, 'Risk review completed. False positive - volume spike was holiday season.', NOW()),

  -- FastCash withdrawal feature disabled
  (RANDOM_UUID(), 'FEATURE_DISABLED', 'MERCHANT', 'a1b2c3d4-0003-0003-0003-000000000003',
   'WITHDRAWALS', NULL, 'riskops',
   NULL, 'Withdrawal disabled pending AML investigation', DATEADD('DAY', -2, NOW()));