create keyspace jepsen WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 3 };
USE jepsen;
create table usertable (
    j_id varchar primary key,
    field0 varchar,
    field1 varchar,
    field2 varchar,
    field3 varchar,
    field4 varchar,
    field5 varchar,
    field6 varchar,
    field7 varchar,
    field8 varchar,
    field9 varchar);
