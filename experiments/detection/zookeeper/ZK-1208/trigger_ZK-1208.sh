cd ~/zookeeper
bin/zkServer.sh stop
rm -rf ~/fuser/zookeeper/version-2
sleep 2

bin/zkServer.sh start
sleep 2
bin/zkServer.sh status

~/T2C-EvalAutomatons/zookeeper/ZK-1208/ZK-1208.sh
