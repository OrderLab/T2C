#run this scripts on all nodes
#this script is hardcoded, need to be paramerized
cd ~/zookeeper
bin/zkServer.sh stop
rm -rf ~/fuser/zookeeper/version-2
sleep 2

bin/zkServer.sh start
sleep 2
bin/zkServer.sh status

~/T2C-EvalAutomatons/zookeeper/ZK-2355/ZK-2355.sh

