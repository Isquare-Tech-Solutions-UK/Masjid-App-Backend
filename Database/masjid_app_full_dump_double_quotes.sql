--
-- PostgreSQL database dump
--

\restrict Fhq8btVIB53OnUeGS9dSL5c0KAyj4n0459oDxX6x8mcSfmaACmf1BATKpwZgTJT

-- Dumped from database version 15.15 (Homebrew)
-- Dumped by pg_dump version 15.15 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = "UTF8";
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config("search_path", "", false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS "cryptographic functions";


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS "generate universally unique identifiers (UUIDs)";


--
-- Name: admin_role; Type: TYPE; Schema: public; Owner: syedfardeen
--

CREATE TYPE public.admin_role AS ENUM (
    "super_admin",
    "admin"
);


ALTER TYPE public.admin_role OWNER TO syedfardeen;

--
-- Name: announcement_status; Type: TYPE; Schema: public; Owner: syedfardeen
--

CREATE TYPE public.announcement_status AS ENUM (
    "draft",
    "scheduled",
    "sent",
    "failed"
);


ALTER TYPE public.announcement_status OWNER TO syedfardeen;

--
-- Name: campaign_status; Type: TYPE; Schema: public; Owner: syedfardeen
--

CREATE TYPE public.campaign_status AS ENUM (
    "draft",
    "active",
    "paused",
    "completed",
    "cancelled"
);


ALTER TYPE public.campaign_status OWNER TO syedfardeen;

--
-- Name: donation_status; Type: TYPE; Schema: public; Owner: syedfardeen
--

CREATE TYPE public.donation_status AS ENUM (
    "pending",
    "completed",
    "failed",
    "refunded"
);


ALTER TYPE public.donation_status OWNER TO syedfardeen;

--
-- Name: event_status; Type: TYPE; Schema: public; Owner: syedfardeen
--

CREATE TYPE public.event_status AS ENUM (
    "draft",
    "published",
    "cancelled",
    "completed"
);


ALTER TYPE public.event_status OWNER TO syedfardeen;

--
-- Name: update_campaign_totals(); Type: FUNCTION; Schema: public; Owner: syedfardeen
--

CREATE FUNCTION public.update_campaign_totals() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- When donation status changes to "completed"
    IF NEW.status = "completed" AND (OLD.status IS NULL OR OLD.status != "completed") THEN
        UPDATE campaigns
        SET 
            raised_amount = raised_amount + NEW.amount,
            donor_count = donor_count + 1,
            updated_at = NOW()
        WHERE id = NEW.campaign_id;
    END IF;
    
    -- When donation is refunded
    IF NEW.status = "refunded" AND OLD.status = "completed" THEN
        UPDATE campaigns
        SET 
            raised_amount = raised_amount - OLD.amount,
            donor_count = donor_count - 1,
            updated_at = NOW()
        WHERE id = NEW.campaign_id;
    END IF;
    
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_campaign_totals() OWNER TO syedfardeen;

--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: syedfardeen
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO syedfardeen;

SET default_tablespace = "";

SET default_table_access_method = heap;

--
-- Name: admin_users; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.admin_users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(100) NOT NULL,
    role public.admin_role DEFAULT "admin"::public.admin_role,
    is_active boolean DEFAULT true,
    failed_login_attempts integer DEFAULT 0,
    locked_until timestamp without time zone,
    last_login_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.admin_users OWNER TO syedfardeen;

--
-- Name: TABLE admin_users; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.admin_users IS "Stores masjid administrator accounts for web portal access";


--
-- Name: COLUMN admin_users.failed_login_attempts; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.admin_users.failed_login_attempts IS "Counter for rate limiting, resets on successful login";


--
-- Name: COLUMN admin_users.locked_until; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.admin_users.locked_until IS "Account locked until this timestamp after 5 failed attempts";


--
-- Name: announcements; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.announcements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(100) NOT NULL,
    message text NOT NULL,
    status public.announcement_status DEFAULT "draft"::public.announcement_status,
    is_scheduled boolean DEFAULT false,
    scheduled_for timestamp without time zone,
    sent_at timestamp without time zone,
    fcm_message_id character varying(255),
    created_by uuid,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.announcements OWNER TO syedfardeen;

--
-- Name: TABLE announcements; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.announcements IS "Push notification announcements - immediate or scheduled";


--
-- Name: COLUMN announcements.scheduled_for; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.announcements.scheduled_for IS "When to send scheduled announcement, NULL for immediate";


--
-- Name: COLUMN announcements.fcm_message_id; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.announcements.fcm_message_id IS "Firebase Cloud Messaging message ID for tracking";


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    action character varying(50) NOT NULL,
    entity_type character varying(50) NOT NULL,
    entity_id uuid NOT NULL,
    old_values jsonb,
    new_values jsonb,
    ip_address inet,
    user_agent text,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.audit_logs OWNER TO syedfardeen;

--
-- Name: TABLE audit_logs; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.audit_logs IS "Audit trail for all admin actions - useful for debugging and compliance";


--
-- Name: COLUMN audit_logs.action; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.audit_logs.action IS "Action type: create, update, delete, login, logout, etc.";


--
-- Name: COLUMN audit_logs.old_values; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.audit_logs.old_values IS "Previous values before update (for update/delete actions)";


--
-- Name: COLUMN audit_logs.new_values; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.audit_logs.new_values IS "New values after change (for create/update actions)";


--
-- Name: campaigns; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.campaigns (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    description text NOT NULL,
    category character varying(50) NOT NULL,
    status public.campaign_status DEFAULT "draft"::public.campaign_status,
    goal_amount numeric(10,2) NOT NULL,
    raised_amount numeric(10,2) DEFAULT 0.00,
    donor_count integer DEFAULT 0,
    start_date date NOT NULL,
    end_date date,
    media jsonb DEFAULT "{}"::jsonb,
    created_by uuid,
    published_at timestamp without time zone,
    ended_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.campaigns OWNER TO syedfardeen;

--
-- Name: TABLE campaigns; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.campaigns IS "Fundraising campaigns with goals, progress tracking, and timeline";


--
-- Name: COLUMN campaigns.raised_amount; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.campaigns.raised_amount IS "Running total updated via triggers or application code";


--
-- Name: COLUMN campaigns.donor_count; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.campaigns.donor_count IS "Count of unique donations (not unique donors if anonymous)";


--
-- Name: donations; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.donations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campaign_id uuid NOT NULL,
    donor_name character varying(100),
    donor_email character varying(255),
    is_anonymous boolean DEFAULT false,
    amount numeric(10,2) NOT NULL,
    processing_fee numeric(10,2) DEFAULT 0.00,
    cover_fee boolean DEFAULT false,
    total_charged numeric(10,2) NOT NULL,
    currency character varying(3) DEFAULT "GBP"::character varying,
    stripe_payment_intent_id character varying(255),
    stripe_checkout_session_id character varying(255),
    payment_method character varying(20),
    status public.donation_status DEFAULT "pending"::public.donation_status,
    receipt_sent boolean DEFAULT false,
    receipt_sent_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.donations OWNER TO syedfardeen;

--
-- Name: TABLE donations; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.donations IS "Individual donation records with Stripe payment tracking";


--
-- Name: COLUMN donations.cover_fee; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.donations.cover_fee IS "True if donor chose to cover processing fees";


--
-- Name: COLUMN donations.total_charged; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.donations.total_charged IS "Final amount charged: amount + fee if cover_fee is true";


--
-- Name: COLUMN donations.stripe_payment_intent_id; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.donations.stripe_payment_intent_id IS "Unique Stripe payment intent - prevents duplicate processing";


--
-- Name: events; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    description text NOT NULL,
    category character varying(50) NOT NULL,
    status public.event_status DEFAULT "draft"::public.event_status,
    event_date date NOT NULL,
    timing jsonb DEFAULT "{}"::jsonb NOT NULL,
    location jsonb DEFAULT "{}"::jsonb NOT NULL,
    speaker jsonb DEFAULT "{}"::jsonb,
    media jsonb DEFAULT "{}"::jsonb,
    registration_link character varying(500),
    notification_sent boolean DEFAULT false,
    notification_sent_at timestamp without time zone,
    created_by uuid,
    published_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.events OWNER TO syedfardeen;

--
-- Name: TABLE events; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.events IS "Community events with JSONB for timing, location, speaker, and media";


--
-- Name: COLUMN events.event_date; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.events.event_date IS "Kept as DATE column for fast calendar queries and indexing";


--
-- Name: COLUMN events.timing; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.events.timing IS "Flexible timing details including start/end times, doors open, etc.";


--
-- Name: COLUMN events.location; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.events.location IS "Venue details, address, capacity, coordinates, accessibility info";


--
-- Name: COLUMN events.speaker; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.events.speaker IS "Speaker info - can be single object or array for multiple speakers";


--
-- Name: COLUMN events.media; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.events.media IS "Images with S3 URLs and keys for featured image and gallery";


--
-- Name: masjid_settings; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.masjid_settings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    about text,
    logo_url character varying(500),
    logo_key character varying(255),
    address_line1 character varying(255),
    address_line2 character varying(255),
    city character varying(100),
    postcode character varying(20),
    country character varying(100) DEFAULT "United Kingdom"::character varying,
    phone character varying(20),
    email character varying(255),
    website character varying(500),
    latitude numeric(10,8),
    longitude numeric(11,8),
    mens_capacity integer,
    womens_capacity integer,
    services jsonb DEFAULT "{}"::jsonb,
    facilities jsonb DEFAULT "{}"::jsonb,
    stripe_account_id character varying(255),
    stripe_account_status character varying(50),
    bank_account_name character varying(255),
    bank_account_number_last4 character varying(4),
    bank_sort_code character varying(10),
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.masjid_settings OWNER TO syedfardeen;

--
-- Name: TABLE masjid_settings; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.masjid_settings IS "Single row table for masjid profile, services, facilities, and payment config";


--
-- Name: COLUMN masjid_settings.services; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.masjid_settings.services IS "JSONB of available services with boolean flags";


--
-- Name: COLUMN masjid_settings.facilities; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.masjid_settings.facilities IS "JSONB of available facilities with boolean flags";


--
-- Name: COLUMN masjid_settings.stripe_account_status; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.masjid_settings.stripe_account_status IS "Status: pending, active, restricted, etc.";


--
-- Name: prayer_times; Type: TABLE; Schema: public; Owner: syedfardeen
--

CREATE TABLE public.prayer_times (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    date date NOT NULL,
    hijri_date character varying(50),
    prayers jsonb NOT NULL,
    jumuah_times jsonb,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.prayer_times OWNER TO syedfardeen;

--
-- Name: TABLE prayer_times; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON TABLE public.prayer_times IS "Daily prayer times with JSONB for flexible prayer structure";


--
-- Name: COLUMN prayer_times.prayers; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.prayer_times.prayers IS "JSON object containing athan and jamah times for all 6 prayers";


--
-- Name: COLUMN prayer_times.jumuah_times; Type: COMMENT; Schema: public; Owner: syedfardeen
--

COMMENT ON COLUMN public.prayer_times.jumuah_times IS "Array of Friday Jumu""ah prayer times, replaces Zuhr on Fridays";


--
-- Data for Name: admin_users; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.admin_users (id, username, email, password_hash, full_name, role, is_active, failed_login_attempts, locked_until, last_login_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: announcements; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.announcements (id, title, message, status, is_scheduled, scheduled_for, sent_at, fcm_message_id, created_by, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.audit_logs (id, user_id, action, entity_type, entity_id, old_values, new_values, ip_address, user_agent, created_at) FROM stdin;
\.


--
-- Data for Name: campaigns; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.campaigns (id, title, description, category, status, goal_amount, raised_amount, donor_count, start_date, end_date, media, created_by, published_at, ended_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: donations; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.donations (id, campaign_id, donor_name, donor_email, is_anonymous, amount, processing_fee, cover_fee, total_charged, currency, stripe_payment_intent_id, stripe_checkout_session_id, payment_method, status, receipt_sent, receipt_sent_at, completed_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.events (id, title, description, category, status, event_date, timing, location, speaker, media, registration_link, notification_sent, notification_sent_at, created_by, published_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: masjid_settings; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.masjid_settings (id, name, about, logo_url, logo_key, address_line1, address_line2, city, postcode, country, phone, email, website, latitude, longitude, mens_capacity, womens_capacity, services, facilities, stripe_account_id, stripe_account_status, bank_account_name, bank_account_number_last4, bank_sort_code, created_at, updated_at) FROM stdin;
914399d8-8138-4004-868b-8da114457fba	My Masjid	\N	\N	\N	\N	\N	\N	\N	United Kingdom	\N	\N	\N	\N	\N	\N	\N	{"iftar": false, "funeral": false, "marriage": false, "counseling": false, "hall_rental": false, "quran_classes": false, "new_muslim_support": false}	{"library": false, "parking": false, "shoe_racks": false, "sisters_area": false, "childrens_area": false, "wudu_facilities": false, "disability_access": false}	\N	\N	\N	\N	\N	2026-01-29 08:15:26.895925	2026-01-29 08:15:26.895925
\.


--
-- Data for Name: prayer_times; Type: TABLE DATA; Schema: public; Owner: syedfardeen
--

COPY public.prayer_times (id, date, hijri_date, prayers, jumuah_times, created_at, updated_at) FROM stdin;
\.


--
-- Name: admin_users admin_users_email_key; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_email_key UNIQUE (email);


--
-- Name: admin_users admin_users_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_pkey PRIMARY KEY (id);


--
-- Name: admin_users admin_users_username_key; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_username_key UNIQUE (username);


--
-- Name: announcements announcements_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.announcements
    ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: campaigns campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_pkey PRIMARY KEY (id);


--
-- Name: donations donations_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.donations
    ADD CONSTRAINT donations_pkey PRIMARY KEY (id);


--
-- Name: donations donations_stripe_payment_intent_id_key; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.donations
    ADD CONSTRAINT donations_stripe_payment_intent_id_key UNIQUE (stripe_payment_intent_id);


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: masjid_settings masjid_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.masjid_settings
    ADD CONSTRAINT masjid_settings_pkey PRIMARY KEY (id);


--
-- Name: prayer_times prayer_times_date_key; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.prayer_times
    ADD CONSTRAINT prayer_times_date_key UNIQUE (date);


--
-- Name: prayer_times prayer_times_pkey; Type: CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.prayer_times
    ADD CONSTRAINT prayer_times_pkey PRIMARY KEY (id);


--
-- Name: idx_admin_users_active; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_admin_users_active ON public.admin_users USING btree (is_active) WHERE (is_active = true);


--
-- Name: idx_admin_users_email; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_admin_users_email ON public.admin_users USING btree (email);


--
-- Name: idx_admin_users_username; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_admin_users_username ON public.admin_users USING btree (username);


--
-- Name: idx_announcements_created_by; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_announcements_created_by ON public.announcements USING btree (created_by);


--
-- Name: idx_announcements_scheduled; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_announcements_scheduled ON public.announcements USING btree (scheduled_for, status) WHERE (status = "scheduled"::public.announcement_status);


--
-- Name: idx_announcements_status; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_announcements_status ON public.announcements USING btree (status, created_at DESC);


--
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);


--
-- Name: idx_audit_logs_created; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_audit_logs_created ON public.audit_logs USING btree (created_at DESC);


--
-- Name: idx_audit_logs_entity; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_type, entity_id);


--
-- Name: idx_audit_logs_user; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_audit_logs_user ON public.audit_logs USING btree (user_id);


--
-- Name: idx_campaigns_active; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_campaigns_active ON public.campaigns USING btree (status, end_date) WHERE (status = "active"::public.campaign_status);


--
-- Name: idx_campaigns_category; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_campaigns_category ON public.campaigns USING btree (category);


--
-- Name: idx_campaigns_created_by; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_campaigns_created_by ON public.campaigns USING btree (created_by);


--
-- Name: idx_campaigns_status; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_campaigns_status ON public.campaigns USING btree (status);


--
-- Name: idx_donations_campaign; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_donations_campaign ON public.donations USING btree (campaign_id);


--
-- Name: idx_donations_completed; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_donations_completed ON public.donations USING btree (campaign_id, status) WHERE (status = "completed"::public.donation_status);


--
-- Name: idx_donations_created; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_donations_created ON public.donations USING btree (created_at DESC);


--
-- Name: idx_donations_status; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_donations_status ON public.donations USING btree (status);


--
-- Name: idx_donations_stripe_payment; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_donations_stripe_payment ON public.donations USING btree (stripe_payment_intent_id);


--
-- Name: idx_donations_stripe_session; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_donations_stripe_session ON public.donations USING btree (stripe_checkout_session_id);


--
-- Name: idx_events_category; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_events_category ON public.events USING btree (category);


--
-- Name: idx_events_created_by; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_events_created_by ON public.events USING btree (created_by);


--
-- Name: idx_events_date; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_events_date ON public.events USING btree (event_date);


--
-- Name: idx_events_published_date; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_events_published_date ON public.events USING btree (event_date, status) WHERE (status = "published"::public.event_status);


--
-- Name: idx_events_status; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_events_status ON public.events USING btree (status);


--
-- Name: idx_prayer_times_date; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_prayer_times_date ON public.prayer_times USING btree (date);


--
-- Name: idx_prayer_times_date_range; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_prayer_times_date_range ON public.prayer_times USING btree (date DESC);


--
-- Name: idx_prayer_times_prayers; Type: INDEX; Schema: public; Owner: syedfardeen
--

CREATE INDEX idx_prayer_times_prayers ON public.prayer_times USING gin (prayers);


--
-- Name: donations trigger_update_campaign_totals; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER trigger_update_campaign_totals AFTER INSERT OR UPDATE ON public.donations FOR EACH ROW EXECUTE FUNCTION public.update_campaign_totals();


--
-- Name: admin_users update_admin_users_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_admin_users_updated_at BEFORE UPDATE ON public.admin_users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: announcements update_announcements_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_announcements_updated_at BEFORE UPDATE ON public.announcements FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: campaigns update_campaigns_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_campaigns_updated_at BEFORE UPDATE ON public.campaigns FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: donations update_donations_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_donations_updated_at BEFORE UPDATE ON public.donations FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: events update_events_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_events_updated_at BEFORE UPDATE ON public.events FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: masjid_settings update_masjid_settings_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_masjid_settings_updated_at BEFORE UPDATE ON public.masjid_settings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: prayer_times update_prayer_times_updated_at; Type: TRIGGER; Schema: public; Owner: syedfardeen
--

CREATE TRIGGER update_prayer_times_updated_at BEFORE UPDATE ON public.prayer_times FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: announcements announcements_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.announcements
    ADD CONSTRAINT announcements_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.admin_users(id);


--
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.admin_users(id);


--
-- Name: campaigns campaigns_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.admin_users(id);


--
-- Name: donations donations_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.donations
    ADD CONSTRAINT donations_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id);


--
-- Name: events events_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: syedfardeen
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.admin_users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict Fhq8btVIB53OnUeGS9dSL5c0KAyj4n0459oDxX6x8mcSfmaACmf1BATKpwZgTJT

