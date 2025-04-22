#! /bin/bash

# make sure this machine can ssh into the other nodes USING SSH KEY and run sudo command
# make sure 

user=dimas
hostname1=node0
hostname2=node1
hostname3=node2
zookeeper_dir=$1

sleep 3

# Stop node 1 2
ssh -t $user@$hostname1 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
ssh -t $user@$hostname2 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; sudo ./bin/zkServer.sh stop"
