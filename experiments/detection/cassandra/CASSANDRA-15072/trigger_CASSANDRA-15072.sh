#!/bin/bash -ex

ccm create test \
--root \
--version="2.1.17" \
--nodes="3" \
--partitioner="org.apache.cassandra.dht.RandomPartitioner"

ccm node1 updateconf 'initial_token: 0'
ccm node2 updateconf 'initial_token: 56713727820156410577229101238628035242'
ccm node3 updateconf 'initial_token: 113427455640312821154458202477256070484'
ccm updateconf

ccm start --root
sleep 15

ccm node1 cqlsh <<SCHEMA
CREATE KEYSPACE test WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 2};
CREATE COLUMNFAMILY test.test (
  id text,
  foo text,
  bar text,
  PRIMARY KEY (id)
) WITH COMPACT STORAGE;
CONSISTENCY ALL;
INSERT INTO test.test (id, foo, bar) values ('1', 'hi', 'there');
INSERT INTO test.test (id, foo, bar) values ('2', 'hi', 'there');
SCHEMA

ccm flush
ccm node1 drain
ccm node2 drain
ccm node3 drain
ccm stop
rm \
.ccm/test/node1/commitlogs/*.log \
.ccm/test/node2/commitlogs/*.log \
.ccm/test/node3/commitlogs/*.log \

ccm start --root
sleep 15

ccm node1 flush
ccm node1 drain
ccm node1 stop
rm .ccm/test/node1/commitlogs/*.log
ccm node1 setdir -v 3.11.4
ccm updateconf
ccm node1 start --root

ccm node3 flush
ccm node3 drain
ccm node3 stop
rm .ccm/test/node3/commitlogs/*.log
ccm node3 setdir -v 3.11.4
ccm updateconf
ccm node3 start --root
sleep 15
ccm flush
ccm node1 drain
ccm node2 drain
ccm node3 drain
ccm stop
rm \
.ccm/test/node1/commitlogs/*.log \
.ccm/test/node2/commitlogs/*.log \
.ccm/test/node3/commitlogs/*.log

ccm start --root

sleep 15

echo "node1:"
cqlsh 127.0.0.1 <<QUERYALL
PAGING 2;
CONSISTENCY ALL;
select * from test.test;
QUERYALL

echo "---"
echo "node2:"

cqlsh 127.0.0.2 <<QUERYALL
PAGING 2;
CONSISTENCY ALL;
select * from test.test;
QUERYALL

echo "---"
echo "node3:"

cqlsh 127.0.0.3 <<QUERYALL
PAGING 2;
CONSISTENCY ALL;
select * from test.test;
QUERYALL

echo "Chevk that the query result is different"