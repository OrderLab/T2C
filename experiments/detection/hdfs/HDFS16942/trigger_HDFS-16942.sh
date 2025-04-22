# run this script on the server, reproducing HDFS14942 only need one server
# $1 should be the directory of Hadoop source code
cd $1
version='3.1.3'

cd hadoop-dist/target/hadoop-${version}/
./bin/hdfs namenode -format
./sbin/hadoop-daemon.sh start namenode
./sbin/hadoop-daemon.sh start datanode
echo "sleep 8 seconds"
sleep 8
echo "stop cluster"
./sbin/hadoop-daemon.sh stop datanode
./sbin/hadoop-daemon.sh stop namenode
echo "now there should be a WARN message 'BR lease 0x{} is not valid for DN{}, because the lease has expired.' in logs/hadoop-username-namenode-hostname.log" 
