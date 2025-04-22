# run this script on the server, reproducing HDFS14514 only need one server
# $1 should be the directory of Hadoop source code
cd $1
version='2.9.2'

cd hadoop-dist/target/hadoop-${version}/
./bin/hdfs namenode -format
./sbin/start-dfs.sh
keytool -genkey
./bin/hadoop key create key1
./sbin/kms.sh start
echo "result of jps:"
jps
./bin/hdfs dfs -mkdir /reproduce
./bin/hdfs crypto -createZone -keyName key1 -path /reproduce
./bin/hdfs dfsadmin -allowSnapshot /reproduce
dd if=/dev/zero of=file_1K bs=1K count=1
./bin/hdfs dfs -put ./file_1K /reproduce
./bin/hdfs dfs -createSnapshot /reproduce snap1
echo "append file_1K"
./bin/hdfs dfs -appendToFile ./file_1K /reproduce/file_1K
echo "Use 'ls' to see the size of file_1K in snapshot. It should be 1K:"
./bin/hdfs dfs -ls /reproduce/.snapshot/snap1/
echo "Use '-get' to get the actual file size, it was different from file_1K."
./bin/hdfs dfs -get /reproduce/.snapshot/snap1/file_1K ./downloaded_file_1K
ls -l ./downloaded_file_1K
echo "The results of two instructions are different"
echo "Bug triggered."
echo "Shutting down Hadoop"
./sbin/stop-dfs.sh
./sbin/kms.sh stop
