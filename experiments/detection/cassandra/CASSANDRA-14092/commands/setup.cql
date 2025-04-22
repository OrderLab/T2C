CREATE KEYSPACE test WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};
CREATE TABLE test.ttlTable (
    pk int PRIMARY KEY,
    value int
);