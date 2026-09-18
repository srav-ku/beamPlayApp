-- ============================================================================
-- Plans become durations: 1 / 3 / 6 months. Free stays as the default plan but is
-- hidden from the pricing screen.
-- ============================================================================
ALTER TABLE plans ADD COLUMN show_in_pricing INTEGER NOT NULL DEFAULT 1;

UPDATE plans SET show_in_pricing = 0 WHERE code = 'free';

-- retire plus/pro (no subscriptions exist yet, so this is safe)
DELETE FROM plan_features WHERE plan_id IN (SELECT id FROM plans WHERE code IN ('plus','pro'));
DELETE FROM plans WHERE code IN ('plus','pro');

INSERT INTO plans (code, name, tagline, price_cents, currency, billing_period,
                   is_featured, max_devices, max_streams, sort_order, show_in_pricing)
VALUES
  ('m1', '1 Month',  '30 days',              9900,  'INR', '30 days', 0, 4, 1, 1, 1),
  ('m3', '3 Months', '90 days',             19900, 'INR', '90 days', 0, 4, 1, 2, 1),
  ('m6', '6 Months', '182 days',            29900, 'INR', '182 days', 1, 4, 1, 3, 1);

-- every paid duration unlocks all three pillars
INSERT INTO plan_features (plan_id, feature_key, enabled, value_int)
SELECT p.id, f.key, 1, -1 FROM plans p, features f WHERE p.code IN ('m1','m3','m6');

-- Verify:
--   SELECT code, name, tagline, price_cents, is_featured, show_in_pricing FROM plans ORDER BY sort_order;
--     -> free (hidden) + 1 Month 9900 + 3 Months 19900 + 6 Months 29900 (featured)
--   SELECT COUNT(*) FROM plan_features;   -- 9 paid + 3 free = 12
-- ============================================================================