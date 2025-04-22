script_path=$(cd "$(dirname "$0")" && pwd)
hbase_path=/localtmp/hbase
hdfs_path=/localtmp/hdfs

export PDSH_RCMD_TYPE=ssh
export HADOOP_HOME=$hdfs_path/hadoop-dist/target/hadoop-3.2.2

cd $hbase_path
./bin/hbase-daemon.sh stop master
./bin/stop-hbase.sh

cd $hbase_path
./bin/hbase-daemon.sh stop regionserver

cd $hdfs_path/hadoop-dist/target/hadoop-3.2.2
./bin/hdfs dfs -rm -r /hbase
./sbin/stop-dfs.sh
rm -r $hbase_path/dataDir
rm -rf /tmp/hadoop-vqx2dc/
sudo kill -9 $(lsof -t -i:9866)
# 564246
# echo "wait 30s for region server to go down"
# sleep 30