-- =====================================================================================
-- Reference data required by every environment.
-- =====================================================================================

INSERT INTO currency (code, name, decimals)
VALUES ('BRL', 'Real brasileiro', 2),
       ('USD', 'Dólar americano', 2);

INSERT INTO receivable_type (code, description, active)
VALUES ('DUPLICATA_MERCANTIL', 'Duplicata Mercantil', TRUE),
       ('CHEQUE_PRE_DATADO', 'Cheque Pré-datado', TRUE);

-- Opening balances of the fund treasury.
INSERT INTO fund_cash_account (currency, balance, version, updated_at)
VALUES ('BRL', 50000000.00, 0, now()),
       ('USD', 10000000.00, 0, now());

-- Bootstrap USD/BRL rate so cross-currency pricing works offline right after the first start.
-- It is dated on the migration day in the business time zone (CURRENT_DATE would follow the
-- session zone, usually UTC, and be "tomorrow" after 21:00 in Sao Paulo). Once it gets older
-- than srm.fx.max-rate-age the engine refuses to use it until a fresh rate arrives.
INSERT INTO exchange_rate (id, base_currency, quote_currency, rate, source, reference_date, created_at)
VALUES (gen_random_uuid(), 'USD', 'BRL', 5.13220000, 'SEED', (now() AT TIME ZONE 'America/Sao_Paulo')::date, now());
