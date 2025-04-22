#run this scripts on all nodes
# sudo ./cleanup.sh <zk absolute dir> <zkbench absolute dir> <master node hostname>

cd $1
sudo ./bin/zkServer.sh stop
rm -rf /tmp/zookeeper/version-2

