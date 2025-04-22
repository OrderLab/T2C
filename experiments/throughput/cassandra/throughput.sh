script_path=$(cd "$(dirname "$0")" && pwd)
echo "script_path: $script_path"
echo "#### checking the status of cassandra cluter, there should be 5 nodes UN"
# add '-h ::FFFF:127.0.0.1' to prevent CASSANDRA-17581
~/cassandra/bin/nodetool -h ::FFFF:127.0.0.1 status 
echo "#### creating test data"
~/cassandra/bin/cqlsh -f ~/YCSB/cassandra/create.sql
echo "#### loading data, check results in the following terminal context"
cd ~/YCSB && python2 bin/ycsb load cassandra-cql -P workloads/workloada -P large.dat -p hosts=localhost -s
echo "#### running workloads/workloada, check ~/YCSB/load.dat for detailed result"
cd ~/YCSB && python2 bin/ycsb run cassandra-cql -P workloads/workloada -P large.dat -p hosts=localhost -s > load.dat
echo "#### running workload_mixed, check ~/YCSB/cassandra_mixed.dat for detailed result"
cd ~/YCSB && python2 bin/ycsb run cassandra-cql -P "${script_path}/../workload_mixed" -P large.dat -p hosts=localhost -s > cassandra_mixed.dat
# echo "#### running workloads/workload_rdonly, check ~/YCSB/cassandra_rdonly.dat for detailed result"
# cd ~/YCSB && python2 bin/ycsb run cassandra-cql -P workloads/workload_rdonly -P large.dat -p hosts=localhost -s > cassandra_rdonly.dat
# echo "#### running workloads/workload_wronly, check ~/YCSB/cassandra_wronly.dat for detailed result"
# cd ~/YCSB && python2 bin/ycsb run cassandra-cql -P workloads/workload_wronly -P large.dat -p hosts=localhost -s > cassandra_wronly.dat
echo "#### throughput experiments ended"