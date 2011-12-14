--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: postgres
--

CREATE OR REPLACE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO postgres;

SET search_path = public, pg_catalog;

--
-- Name: events_id_seq; Type: SEQUENCE; Schema: public; Owner: timeline
--

CREATE SEQUENCE events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.events_id_seq OWNER TO timeline;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: event; Type: TABLE; Schema: public; Owner: timeline; Tablespace: 
--

CREATE TABLE event (
    id integer DEFAULT nextval('events_id_seq'::regclass) NOT NULL,
    startdate timestamp without time zone NOT NULL,
    enddate timestamp without time zone,
    title character varying(140),
    description character varying,
    link character varying(512),
    importance integer,
    parent integer,
    start_date_format character varying(20),
    end_date_format character varying(20),
    user_id character varying(50)
);


ALTER TABLE public.event OWNER TO timeline;

--
-- Name: tag; Type: TABLE; Schema: public; Owner: timeline; Tablespace: 
--

CREATE TABLE tag (
    tag character varying NOT NULL,
    event_id integer NOT NULL
);


ALTER TABLE public.tag OWNER TO timeline;

--
-- Name: uploads; Type: TABLE; Schema: public; Owner: timeline; Tablespace: 
--

CREATE TABLE uploads (
    event_id integer NOT NULL,
    filename character varying NOT NULL
);


ALTER TABLE public.uploads OWNER TO timeline;

--
-- Name: users; Type: TABLE; Schema: public; Owner: timeline; Tablespace: 
--

CREATE TABLE users (
    username character varying(50) NOT NULL,
    email character varying(255) NOT NULL,
    hash character varying(60) NOT NULL
);


ALTER TABLE public.users OWNER TO timeline;

--
-- Name: events_pkey; Type: CONSTRAINT; Schema: public; Owner: timeline; Tablespace: 
--

ALTER TABLE ONLY event
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: tags_pkey; Type: CONSTRAINT; Schema: public; Owner: timeline; Tablespace: 
--

ALTER TABLE ONLY tag
    ADD CONSTRAINT tags_pkey PRIMARY KEY (tag, event_id);


--
-- Name: uploads_pkey; Type: CONSTRAINT; Schema: public; Owner: timeline; Tablespace: 
--

ALTER TABLE ONLY uploads
    ADD CONSTRAINT uploads_pkey PRIMARY KEY (event_id, filename);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: timeline; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);


--
-- Name: event_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: timeline
--

ALTER TABLE ONLY event
    ADD CONSTRAINT event_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(username);


--
-- Name: tags_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: timeline
--

ALTER TABLE ONLY tag
    ADD CONSTRAINT tags_event_id_fkey FOREIGN KEY (event_id) REFERENCES event(id);


--
-- Name: uploads_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: timeline
--

ALTER TABLE ONLY uploads
    ADD CONSTRAINT uploads_event_id_fkey FOREIGN KEY (event_id) REFERENCES event(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

