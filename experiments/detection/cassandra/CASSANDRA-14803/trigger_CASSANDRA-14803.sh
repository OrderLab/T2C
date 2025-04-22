#! /bin/bash
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit # or "$1"

# Change cassandra.yaml
sed -i "s/partitioner:.*/partitioner: org.apache.cassandra.dht.ByteOrderedPartitioner/g" ./conf/cassandra.yaml

# Run cassandra, wait 2s, check whether cassandra runs successfully
./bin/cassandra || exit
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status || exit

echo "Executing queries"

./bin/cqlsh -f "$script_dir/commands/setup.cql" || exit

cp -r $script_dir/legacy_ka_14803/. ./data/data/legacy_tables/legacy*

./bin/nodetool -h ::FFFF:127.0.0.1 refresh -- legacy_tables legacy_ka_14803

./bin/cqlsh -f "$script_dir/commands/query.cql" || exit

echo "Check that the second query gives the wrong result"