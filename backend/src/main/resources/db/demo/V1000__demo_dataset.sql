-- =====================================================================================
-- Demo dataset - only applied with the "demo" profile (spring.flyway.locations adds
-- classpath:db/demo). Volume for exercising the settlement statement:
--   * 200 assignors
--   * 100,000 credit assignments spread over the last 12 months
--     (60% SETTLED, 30% PENDING, 10% CANCELLED; 80% paid in BRL, 20% in USD)
--   * 1 to 3 receivables each (~200,000), ~10% of them cross-currency
--   * one ledger DEBIT per settled operation
--
-- Deterministic on purpose (no random()): every run produces the same distribution.
-- Amounts follow the pricing formula PV = face / (1 + baseRate + spread) ^ (days / 30) and
-- totals are consistent (face = discount + net). SQL round() is half away from zero, while
-- the application rounds HALF_EVEN; cents may differ on exact ties, irrelevant for demo data.
-- Demo documents start with 7700 and carry valid check digits, so they pass the same CNPJ
-- validation as the API and the UI (they are not registered companies).
-- =====================================================================================

-- CNPJ check digits: modulo 11 with weights 5,4,3,2,9..2 (1st digit) and 6,5,4,3,2,9..2 (2nd digit).
-- Session-scoped (pg_temp): the helper does not outlive the migration.
CREATE FUNCTION pg_temp.cnpj_with_check_digits(base12 text) RETURNS text
    LANGUAGE sql
    IMMUTABLE AS
$$
WITH first_digit AS (SELECT base12 || (CASE WHEN s % 11 < 2 THEN 0 ELSE 11 - s % 11 END) AS base13
                     FROM (SELECT sum(substr(base12, i, 1)::int * (ARRAY [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])[i]) AS s
                           FROM generate_series(1, 12) AS i) AS weighted)
SELECT base13 || (CASE WHEN s % 11 < 2 THEN 0 ELSE 11 - s % 11 END)
FROM first_digit,
     LATERAL (SELECT sum(substr(base13, i, 1)::int * (ARRAY [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])[i]) AS s
              FROM generate_series(1, 13) AS i) AS weighted
$$;

INSERT INTO assignor (id, name, document, created_at)
SELECT gen_random_uuid(),
       format('%s %s %s %s',
              (ARRAY ['Alfa', 'Beta', 'Gama', 'Delta', 'Sigma', 'Ômega', 'Atlas', 'Orion', 'Vega', 'Lyra'])[1 + g % 10],
              (ARRAY ['Indústria', 'Comércio', 'Distribuidora', 'Agronegócio', 'Logística', 'Têxtil', 'Alimentos',
                      'Metalúrgica'])[1 + (g / 10) % 8],
              lpad(g::text, 3, '0'),
              (ARRAY ['Ltda', 'S.A.', 'Eireli', 'ME'])[1 + g % 4]),
       pg_temp.cnpj_with_check_digits(lpad((77000000 + g)::text, 8, '0') || '0001'),
       now() - interval '400 days' + g * interval '1 hour'
FROM generate_series(1, 200) AS g;

CREATE TEMPORARY TABLE demo_assignor ON COMMIT DROP AS
SELECT id, row_number() OVER (ORDER BY document) - 1 AS rn
FROM assignor
WHERE document LIKE '7700%';

-- Operation headers (totals are computed from the receivables below).
CREATE TEMPORARY TABLE demo_operation ON COMMIT DROP AS
SELECT g                                                              AS seq,
       gen_random_uuid()                                              AS id,
       a.id                                                           AS assignor_id,
       CASE WHEN g % 5 = 0 THEN 'USD' ELSE 'BRL' END                  AS payment_currency,
       -- (g / 7) keeps the status independent from the currency (which uses g % 5)
       CASE WHEN (g / 7) % 10 < 6 THEN 'SETTLED' WHEN (g / 7) % 10 < 9 THEN 'PENDING' ELSE 'CANCELLED' END AS status,
       now() - ((g::bigint * 7919) % (365 * 86400)) * interval '1 second' AS created_at,
       1 + g % 3                                                      AS receivables_count
FROM generate_series(1, 100000) AS g
         JOIN demo_assignor a ON a.rn = g % 200;

-- Priced receivables, converted with the latest USD/BRL rate available at migration time.
CREATE TEMPORARY TABLE demo_receivable ON COMMIT DROP AS
WITH fx AS (SELECT id, rate
            FROM exchange_rate
            WHERE base_currency = 'USD'
              AND quote_currency = 'BRL'
            ORDER BY reference_date DESC, created_at DESC
            LIMIT 1),
     items AS (SELECT o.id                                                                    AS operation_id,
                      o.payment_currency,
                      o.created_at,
                      CASE WHEN (o.seq + n) % 3 = 0 THEN 'CHEQUE_PRE_DATADO' ELSE 'DUPLICATA_MERCANTIL' END AS type_code,
                      CASE
                          WHEN (o.seq * 3 + n) % 10 = 0
                              THEN CASE o.payment_currency WHEN 'BRL' THEN 'USD' ELSE 'BRL' END
                          ELSE o.payment_currency END                                     AS face_currency,
                      round(1000 + ((o.seq::bigint * 104729 + n * 7919) % 49900000) / 100.0, 2) AS face_value,
                      15 + ((o.seq * 37 + n * 101) % 350)                                 AS term_days
               FROM demo_operation o
                        CROSS JOIN LATERAL generate_series(1, o.receivables_count) AS n),
     priced AS (SELECT i.*,
                       CASE i.face_currency WHEN 'BRL' THEN 0.01 ELSE 0.005 END             AS base_rate,
                       CASE i.type_code WHEN 'DUPLICATA_MERCANTIL' THEN 0.015 ELSE 0.025 END AS spread
                FROM items i),
     valued AS (SELECT p.*,
                       p.face_value / power(1 + p.base_rate + p.spread, p.term_days / 30.0) AS pv_exact
                FROM priced p)
SELECT gen_random_uuid()                                                        AS id,
       v.operation_id,
       v.type_code,
       v.face_value,
       v.face_currency,
       (v.created_at AT TIME ZONE 'America/Sao_Paulo')::date + v.term_days      AS due_date,
       v.term_days,
       v.base_rate,
       v.spread,
       round(v.pv_exact, 2)                                                     AS present_value,
       v.face_value - round(v.pv_exact, 2)                                      AS discount,
       CASE WHEN v.face_currency <> v.payment_currency THEN fx.id END           AS exchange_rate_id,
       CASE WHEN v.face_currency <> v.payment_currency THEN fx.rate END         AS exchange_rate_applied,
       round(CASE
                 WHEN v.face_currency = v.payment_currency THEN v.pv_exact
                 WHEN v.face_currency = 'USD' THEN v.pv_exact * fx.rate
                 ELSE v.pv_exact / fx.rate END, 2)                              AS net_amount,
       round(CASE
                 WHEN v.face_currency = v.payment_currency THEN v.face_value
                 WHEN v.face_currency = 'USD' THEN v.face_value * fx.rate
                 ELSE v.face_value / fx.rate END, 2)                            AS face_in_payment_currency
FROM valued v
         CROSS JOIN fx;

INSERT INTO credit_assignment (id, assignor_id, payment_currency, status, total_face_value, total_discount,
                               total_net_amount, receivables_count, version, created_at, settled_at, cancelled_at)
SELECT o.id,
       o.assignor_id,
       o.payment_currency,
       o.status,
       t.total_face,
       t.total_face - t.total_net,
       t.total_net,
       o.receivables_count,
       CASE WHEN o.status = 'PENDING' THEN 0 ELSE 1 END,
       o.created_at,
       CASE WHEN o.status = 'SETTLED' THEN least(o.created_at + interval '2 hours', now()) END,
       CASE WHEN o.status = 'CANCELLED' THEN least(o.created_at + interval '1 day', now()) END
FROM demo_operation o
         JOIN (SELECT operation_id, sum(face_in_payment_currency) AS total_face, sum(net_amount) AS total_net
               FROM demo_receivable
               GROUP BY operation_id) t ON t.operation_id = o.id;

INSERT INTO receivable (id, credit_assignment_id, type_code, face_value, face_currency, due_date, term_days,
                        base_rate, spread, present_value, discount, exchange_rate_id, exchange_rate_applied,
                        net_amount)
SELECT id,
       operation_id,
       type_code,
       face_value,
       face_currency,
       due_date,
       term_days,
       base_rate,
       spread,
       present_value,
       discount,
       exchange_rate_id,
       exchange_rate_applied,
       net_amount
FROM demo_receivable;

-- Ledger: one debit per settled operation...
INSERT INTO cash_movement (id, currency, credit_assignment_id, amount, direction, created_at)
SELECT gen_random_uuid(), ca.payment_currency, ca.id, ca.total_net_amount, 'DEBIT', ca.settled_at
FROM credit_assignment ca
         JOIN demo_operation o ON o.id = ca.id
WHERE ca.status = 'SETTLED';

-- ...funded by one capital contribution per currency, so the opening balances of the fund
-- stay untouched and the ledger still reconciles (balance = opening + credits - debits).
INSERT INTO cash_movement (id, currency, credit_assignment_id, amount, direction, created_at)
SELECT gen_random_uuid(), m.currency, NULL, sum(m.amount), 'CREDIT', now() - interval '366 days'
FROM cash_movement m
         JOIN demo_operation o ON o.id = m.credit_assignment_id
GROUP BY m.currency;

ANALYZE assignor;
ANALYZE credit_assignment;
ANALYZE receivable;
ANALYZE cash_movement;
