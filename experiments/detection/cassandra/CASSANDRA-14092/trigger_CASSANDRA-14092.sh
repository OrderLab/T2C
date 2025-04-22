#! /bin/bash
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit # or "$1"

# Run cassandra, wait 2s, check whether cassandra runs successfully
./bin/cassandra || exit
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status || exit

echo "Executing queries"

./bin/cqlsh -f "$script_dir/commands/setup.cql" || exit
./bin/cqlsh -f "$script_dir/commands/query.cql" || exit

echo "Check that (2,2) will not exists because of the bug"