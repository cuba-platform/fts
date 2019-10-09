-- begin FTS_TEST_MAIN_ENTITY
create table FTS_TEST_MAIN_ENTITY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    RLS varchar(255),
    DESCRIPTION varchar(255),
    RELATION_ID varchar(36),
    --
    primary key (ID)
)^
-- end FTS_TEST_MAIN_ENTITY
-- begin FTS_TEST_RELATED_ENTITY
create table FTS_TEST_RELATED_ENTITY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    DESCRIPTION varchar(255),
    RLS varchar(255),
    --
    primary key (ID)
)^
-- end FTS_TEST_RELATED_ENTITY
-- begin FTS_TEST_BOOK
create table FTS_TEST_BOOK (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TITLE varchar(255),
    BOOK_FILE_ID varchar(36),
    --
    primary key (ID)
)^
-- end FTS_TEST_BOOK
-- begin FTS_TEST_BOOK_REVIEW
create table FTS_TEST_BOOK_REVIEW (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    REVIEW_FILE_ID varchar(36),
    BOOK_ID varchar(36),
    --
    primary key (ID)
)^

