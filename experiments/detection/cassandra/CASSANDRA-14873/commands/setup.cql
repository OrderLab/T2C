CREATE KEYSPACE legacy_tables WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE legacy_tables.legacy_ka_14873 (pkc int, cc int, sc int static, rc text, PRIMARY KEY (pkc, cc)) WITH CLUSTERING ORDER BY (cc DESC) AND compaction = {'enabled' : 'false', 'class' : 'LeveledCompactionStrategy'};
