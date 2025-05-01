script_path=$(cd "$(dirname "$0")" && pwd)
hbase_path=/localtmp/hbase
hdfs_path=~/hdfs_for_hbase

# HADOOP_COMMON_HOME=$hdfs_path/hadoop-dist/target/hadoop-3.2.2/
# cd /home/dimas/T2C-EvalAutomatons/throughput_eval/hbase/
echo "script_path: $script_path"
cd $hbase_path
./bin/start-hbase.sh
sleep 10
# echo "status" | ./bin/hbase shell
# cd /home/dimas/T2C-EvalAutomatons/throughput_eval/hbase/
# cd $hbase_path
# ./bin/hbase-daemon.sh stop regionserver
# cd /home/dimas/T2C-EvalAutomatons/throughput_eval/hbase/
# sleep 20
echo "#### checking the status of hbase cluster:"
echo "status" | ./bin/hbase shell
echo "#### creating test data"
cd $hbase_path
# export _JAVA_OPTIONS="-Xmx24g"
# unset _JAVA_OPTIONS
# ./bin/hbase shell ~/YCSB/hbase10/create.txt
./bin/hbase shell /u/vqx2dc/jepsen/hbase/create.txt
echo "#### loading data, check results in the following terminal context"
cd ~/YCSB && python2 bin/ycsb load hbase10 -P workloads/workloada -P large.dat -s -cp ~/hbase/conf -p table=usertable -p columnfamily=family
echo "#### running workloads/workloada, check ~/YCSB/load.dat for detailed result"
cd ~/YCSB && python2 bin/ycsb run hbase10 -P workloads/workloada -P large.dat -s -cp ~/hbase/conf -p table=usertable -p columnfamily=family > load.dat
echo "#### running workload_mixed, check ~/YCSB/hbase_mixed.dat for detailed result"
cd ~/YCSB && python2 bin/ycsb run hbase10 -P "${script_path}/../workload_mixed" -P large.dat -s -cp ~/hbase/conf -p table=usertable -p columnfamily=family > hbase_mixed.dat
# echo "#### running workloads/workload_rdonly, check ~/YCSB/hbase_rdonly.dat for detailed result"
# cd ~/YCSB && python2 bin/ycsb run hbase10 -P workloads/workload_rdonly -P large.dat -s -cp ~/hbase/conf -p table=usertable -p columnfamily=family > hbase_rdonly.dat
# echo "#### running workloads/workload_wronly, check ~/YCSB/hbase_wronly.dat for detailed result"
# cd ~/YCSB && python2 bin/ycsb run hbase10 -P workloads/workload_wronly -P large.dat -s -cp ~/hbase/conf -p table=usertable -p columnfamily=family > hbase_wronly.dat
echo "#### throughput experiments ended"