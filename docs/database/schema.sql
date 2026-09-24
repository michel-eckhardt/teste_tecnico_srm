-- =====================================================================================
-- SRM Credit Engine - DDL (PostgreSQL 17)
--
-- Snapshot of the schema produced by the Flyway migrations, for reading and review.
-- Source of truth: backend/src/main/resources/db/migration (V1 schema, V2 reference data).
-- Regenerate with the compose stack running:
--   docker compose exec -T postgres pg_dump -U srm -d srm_credit_engine --schema-only \
--     --no-owner --no-privileges --exclude-table=flyway_schema_history
-- =====================================================================================

--
-- Name: assignor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assignor (
    id uuid NOT NULL,
    name character varying(150) NOT NULL,
    document character varying(14) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_assignor_document_digits CHECK (((document)::text ~ '^[0-9]{14}$'::text))
);

--
-- Name: COLUMN assignor.document; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.assignor.document IS 'CNPJ, digits only (check digits are validated by the application).';

--
-- Name: cash_movement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cash_movement (
    id uuid NOT NULL,
    currency character varying(3) NOT NULL,
    credit_assignment_id uuid,
    amount numeric(19,2) NOT NULL,
    direction character varying(6) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_cash_movement_amount_positive CHECK ((amount > (0)::numeric)),
    CONSTRAINT ck_cash_movement_debit_has_operation CHECK ((((direction)::text = 'CREDIT'::text) OR (credit_assignment_id IS NOT NULL))),
    CONSTRAINT ck_cash_movement_direction CHECK (((direction)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[])))
);

--
-- Name: TABLE cash_movement; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cash_movement IS 'Append-only ledger of the fund cash accounts.';

--
-- Name: credit_assignment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credit_assignment (
    id uuid NOT NULL,
    assignor_id uuid NOT NULL,
    payment_currency character varying(3) NOT NULL,
    status character varying(20) NOT NULL,
    total_face_value numeric(19,2) NOT NULL,
    total_discount numeric(19,2) NOT NULL,
    total_net_amount numeric(19,2) NOT NULL,
    receivables_count integer NOT NULL,
    idempotency_key character varying(100),
    request_hash character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    settled_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    CONSTRAINT ck_credit_assignment_idempotency CHECK (((idempotency_key IS NULL) = (request_hash IS NULL))),
    CONSTRAINT ck_credit_assignment_lifecycle CHECK (((((status)::text = 'PENDING'::text) AND (settled_at IS NULL) AND (cancelled_at IS NULL)) OR (((status)::text = 'SETTLED'::text) AND (settled_at IS NOT NULL) AND (cancelled_at IS NULL)) OR (((status)::text = 'CANCELLED'::text) AND (cancelled_at IS NOT NULL) AND (settled_at IS NULL)))),
    CONSTRAINT ck_credit_assignment_receivables_count CHECK ((receivables_count > 0)),
    CONSTRAINT ck_credit_assignment_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SETTLED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT ck_credit_assignment_totals CHECK (((total_face_value > (0)::numeric) AND (total_discount >= (0)::numeric) AND (total_net_amount >= (0)::numeric) AND (total_face_value = (total_discount + total_net_amount))))
);

--
-- Name: TABLE credit_assignment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.credit_assignment IS 'Credit assignment operation (batch of receivables bought from an assignor).';

--
-- Name: COLUMN credit_assignment.total_face_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_assignment.total_face_value IS 'Sum of face values converted to the payment currency (denormalized for the statement report).';

--
-- Name: COLUMN credit_assignment.total_net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_assignment.total_net_amount IS 'Amount paid to the assignor in the payment currency (sum of receivable net amounts).';

--
-- Name: COLUMN credit_assignment.request_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_assignment.request_hash IS 'SHA-256 of the canonical creation request, used to detect Idempotency-Key reuse with another payload.';

--
-- Name: COLUMN credit_assignment.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_assignment.version IS 'Optimistic locking version, exposed to clients as the ETag.';

--
-- Name: currency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.currency (
    code character varying(3) NOT NULL,
    name character varying(60) NOT NULL,
    decimals smallint NOT NULL,
    CONSTRAINT ck_currency_code_iso4217 CHECK (((code)::text ~ '^[A-Z]{3}$'::text)),
    CONSTRAINT ck_currency_decimals CHECK (((decimals >= 0) AND (decimals <= 4)))
);

--
-- Name: TABLE currency; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.currency IS 'ISO-4217 currencies the fund operates with (payment and face currencies).';

--
-- Name: COLUMN currency.decimals; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.currency.decimals IS 'Minor units used when rounding amounts expressed in this currency.';

--
-- Name: exchange_rate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exchange_rate (
    id uuid NOT NULL,
    base_currency character varying(3) NOT NULL,
    quote_currency character varying(3) NOT NULL,
    rate numeric(19,8) NOT NULL,
    source character varying(20) NOT NULL,
    reference_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_exchange_rate_distinct_currencies CHECK (((base_currency)::text <> (quote_currency)::text)),
    CONSTRAINT ck_exchange_rate_positive CHECK ((rate > (0)::numeric)),
    CONSTRAINT ck_exchange_rate_source CHECK (((source)::text = ANY ((ARRAY['MANUAL'::character varying, 'FRANKFURTER'::character varying, 'SEED'::character varying])::text[])))
);

--
-- Name: TABLE exchange_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.exchange_rate IS 'Append-only FX history. Rows are never updated so every priced receivable can reference the exact rate it used.';

--
-- Name: COLUMN exchange_rate.rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exchange_rate.rate IS 'Units of quote_currency worth one unit of base_currency (USD/BRL 5.1322 => 1 USD = 5.1322 BRL).';

--
-- Name: COLUMN exchange_rate.reference_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exchange_rate.reference_date IS 'Business date the rate refers to (e.g. the ECB publication date).';

--
-- Name: fund_cash_account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fund_cash_account (
    currency character varying(3) NOT NULL,
    balance numeric(19,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_fund_cash_account_balance_non_negative CHECK ((balance >= (0)::numeric))
);

--
-- Name: COLUMN fund_cash_account.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fund_cash_account.version IS 'Optimistic locking version: concurrent debits never overwrite each other.';

--
-- Name: receivable; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.receivable (
    id uuid NOT NULL,
    credit_assignment_id uuid NOT NULL,
    type_code character varying(40) NOT NULL,
    face_value numeric(19,2) NOT NULL,
    face_currency character varying(3) NOT NULL,
    due_date date NOT NULL,
    term_days integer NOT NULL,
    base_rate numeric(19,8) NOT NULL,
    spread numeric(19,8) NOT NULL,
    present_value numeric(19,2) NOT NULL,
    discount numeric(19,2) NOT NULL,
    exchange_rate_id uuid,
    exchange_rate_applied numeric(19,8),
    net_amount numeric(19,2) NOT NULL,
    CONSTRAINT ck_receivable_amounts CHECK (((present_value >= (0)::numeric) AND (discount >= (0)::numeric) AND (net_amount >= (0)::numeric) AND (face_value = (present_value + discount)))),
    CONSTRAINT ck_receivable_face_value_positive CHECK ((face_value > (0)::numeric)),
    CONSTRAINT ck_receivable_fx_snapshot CHECK (((exchange_rate_id IS NULL) = (exchange_rate_applied IS NULL))),
    CONSTRAINT ck_receivable_rates_non_negative CHECK (((base_rate >= (0)::numeric) AND (spread >= (0)::numeric))),
    CONSTRAINT ck_receivable_term_positive CHECK ((term_days > 0))
);

--
-- Name: TABLE receivable; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.receivable IS 'Priced receivable with a full audit snapshot of the rates used (base rate, spread, FX).';

--
-- Name: COLUMN receivable.present_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.receivable.present_value IS 'face_value / (1 + base_rate + spread) ^ (term_days / 30), in the face currency.';

--
-- Name: COLUMN receivable.exchange_rate_applied; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.receivable.exchange_rate_applied IS 'Rate of the referenced exchange_rate row at pricing time (quote per base of that row).';

--
-- Name: COLUMN receivable.net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.receivable.net_amount IS 'Present value converted to the payment currency of the operation.';

--
-- Name: receivable_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.receivable_type (
    code character varying(40) NOT NULL,
    description character varying(120) NOT NULL,
    active boolean DEFAULT true NOT NULL
);

--
-- Name: TABLE receivable_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.receivable_type IS 'Receivable products. The risk spread of each type lives in a pricing strategy (code), not here.';

--
-- Name: assignor pk_assignor; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignor
    ADD CONSTRAINT pk_assignor PRIMARY KEY (id);

--
-- Name: cash_movement pk_cash_movement; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_movement
    ADD CONSTRAINT pk_cash_movement PRIMARY KEY (id);

--
-- Name: credit_assignment pk_credit_assignment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_assignment
    ADD CONSTRAINT pk_credit_assignment PRIMARY KEY (id);

--
-- Name: currency pk_currency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.currency
    ADD CONSTRAINT pk_currency PRIMARY KEY (code);

--
-- Name: exchange_rate pk_exchange_rate; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_rate
    ADD CONSTRAINT pk_exchange_rate PRIMARY KEY (id);

--
-- Name: fund_cash_account pk_fund_cash_account; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fund_cash_account
    ADD CONSTRAINT pk_fund_cash_account PRIMARY KEY (currency);

--
-- Name: receivable pk_receivable; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receivable
    ADD CONSTRAINT pk_receivable PRIMARY KEY (id);

--
-- Name: receivable_type pk_receivable_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receivable_type
    ADD CONSTRAINT pk_receivable_type PRIMARY KEY (code);

--
-- Name: assignor uk_assignor_document; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignor
    ADD CONSTRAINT uk_assignor_document UNIQUE (document);

--
-- Name: credit_assignment uk_credit_assignment_idempotency_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_assignment
    ADD CONSTRAINT uk_credit_assignment_idempotency_key UNIQUE (idempotency_key);

--
-- Name: ix_cash_movement_currency_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_cash_movement_currency_created_at ON public.cash_movement USING btree (currency, created_at DESC);

--
-- Name: ix_credit_assignment_assignor_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_credit_assignment_assignor_created_at ON public.credit_assignment USING btree (assignor_id, created_at DESC);

--
-- Name: ix_credit_assignment_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_credit_assignment_created_at ON public.credit_assignment USING btree (created_at DESC, id DESC);

--
-- Name: ix_credit_assignment_currency_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_credit_assignment_currency_created_at ON public.credit_assignment USING btree (payment_currency, created_at DESC);

--
-- Name: ix_credit_assignment_status_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_credit_assignment_status_created_at ON public.credit_assignment USING btree (status, created_at DESC);

--
-- Name: ix_exchange_rate_pair_latest; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_exchange_rate_pair_latest ON public.exchange_rate USING btree (base_currency, quote_currency, reference_date DESC, created_at DESC);

--
-- Name: ix_exchange_rate_quote_currency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_exchange_rate_quote_currency ON public.exchange_rate USING btree (quote_currency);

--
-- Name: ix_receivable_credit_assignment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_receivable_credit_assignment ON public.receivable USING btree (credit_assignment_id);

--
-- Name: ix_receivable_exchange_rate; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_receivable_exchange_rate ON public.receivable USING btree (exchange_rate_id) WHERE (exchange_rate_id IS NOT NULL);

--
-- Name: uq_cash_movement_settlement_debit; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_cash_movement_settlement_debit ON public.cash_movement USING btree (credit_assignment_id) WHERE ((direction)::text = 'DEBIT'::text);

--
-- Name: cash_movement fk_cash_movement_account; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_movement
    ADD CONSTRAINT fk_cash_movement_account FOREIGN KEY (currency) REFERENCES public.fund_cash_account(currency);

--
-- Name: cash_movement fk_cash_movement_credit_assignment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_movement
    ADD CONSTRAINT fk_cash_movement_credit_assignment FOREIGN KEY (credit_assignment_id) REFERENCES public.credit_assignment(id);

--
-- Name: credit_assignment fk_credit_assignment_assignor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_assignment
    ADD CONSTRAINT fk_credit_assignment_assignor FOREIGN KEY (assignor_id) REFERENCES public.assignor(id);

--
-- Name: credit_assignment fk_credit_assignment_payment_currency; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_assignment
    ADD CONSTRAINT fk_credit_assignment_payment_currency FOREIGN KEY (payment_currency) REFERENCES public.currency(code);

--
-- Name: exchange_rate fk_exchange_rate_base_currency; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_rate
    ADD CONSTRAINT fk_exchange_rate_base_currency FOREIGN KEY (base_currency) REFERENCES public.currency(code);

--
-- Name: exchange_rate fk_exchange_rate_quote_currency; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_rate
    ADD CONSTRAINT fk_exchange_rate_quote_currency FOREIGN KEY (quote_currency) REFERENCES public.currency(code);

--
-- Name: fund_cash_account fk_fund_cash_account_currency; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fund_cash_account
    ADD CONSTRAINT fk_fund_cash_account_currency FOREIGN KEY (currency) REFERENCES public.currency(code);

--
-- Name: receivable fk_receivable_credit_assignment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receivable
    ADD CONSTRAINT fk_receivable_credit_assignment FOREIGN KEY (credit_assignment_id) REFERENCES public.credit_assignment(id);

--
-- Name: receivable fk_receivable_exchange_rate; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receivable
    ADD CONSTRAINT fk_receivable_exchange_rate FOREIGN KEY (exchange_rate_id) REFERENCES public.exchange_rate(id);

--
-- Name: receivable fk_receivable_face_currency; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receivable
    ADD CONSTRAINT fk_receivable_face_currency FOREIGN KEY (face_currency) REFERENCES public.currency(code);

--
-- Name: receivable fk_receivable_type; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receivable
    ADD CONSTRAINT fk_receivable_type FOREIGN KEY (type_code) REFERENCES public.receivable_type(code);

--
--

