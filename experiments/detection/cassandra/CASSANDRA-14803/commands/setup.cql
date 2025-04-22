CREATE KEYSPACE legacy_tables WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE legacy_tables.legacy_ka_14803 (k int, c int, v1 blob, v2 blob, PRIMARY KEY (k, c));
