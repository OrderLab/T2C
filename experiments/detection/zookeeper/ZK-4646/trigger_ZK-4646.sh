#! /bin/bash

# make sure this machine can ssh into the other nodes USING SSH KEY and run sudo command
# make sure 

user=dimas
hostname1=node0
hostname2=node1
hostname3=node2
zookeeper_dir=$1

# Start zk in order 1 3 2
ssh -t $user@$hostname1 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
sleep 5
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
sleep 5
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
sleep 5

# Create node in node 3
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; nohup sudo ./bin/zkCli.sh create /test ok > /dev/null"

# Stop node 1 2
ssh -t $user@$hostname1 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
sleep 3

# Start node 1 2
ssh -t $user@$hostname1 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
sleep 3

# Check whether node still exist in node 3
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; nohup sudo ./bin/zkCli.sh ls / > /dev/null"


# Stop node 2 1
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
ssh -t $user@$hostname1 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
sleep 3

# Start node 1 2
ssh -t $user@$hostname1 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh start"
sleep 3

# Stop node 3
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
sleep 3

# Check whether node exists in node 2. If the bug occurs, then it shouldn't exist
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkCli.sh ls /"

echo -e "////////////////////////////////////////////////\n"

echo "Check that there is only 1 node [zookeeper]"
echo "/test node is missing because of the bug"

echo -e "\n////////////////////////////////////////////////\n"