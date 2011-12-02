create sequence events_id_seq
  start with 1
  increment by 1
  no maxvalue
  no minvalue
  cache 1;

create table events (
    id integer DEFAULT nextval('events_id_seq'::regclass) primary key,
    startdate timestamp without time zone not null,
    enddate timestamp without time zone,
    title character varying (140),
    description character varying,
    link character varying (512),
    importance integer
);

create table tags (
    tag character varying (40) primary key
);

create sequence tags_to_events_id_seq
  start with 1
  increment by 1
  no maxvalue
  no minvalue
  cache 1;

create table tags_to_events (
    id integer default nextval('tags_to_events_id_seq'::regclass) primary key,
    event integer references events(id),
    tag character varying (40) references tags(tag)
);