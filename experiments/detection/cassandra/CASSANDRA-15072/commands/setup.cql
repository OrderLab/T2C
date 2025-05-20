CREATE KEYSPACE test WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 2};
CREATE COLUMNFAMILY test.ttlTable (
  id text,
  foo text,
  bar text,
  PRIMARY KEY (id)
) WITH COMPACT STORAGE;
CONSISTENCY ALL;
INSERT INTO test.ttlTable (id, foo, bar) values ('1', 'hi', 'there');
INSERT INTO test.ttlTable (id, foo, bar) values ('2', 'hi', 'there');