#! /bin/bash

# make sure this machine can ssh into the other nodes USING SSH KEY and run sudo command
# make sure 

user=dimas
hostname1=node0
hostname2=node1
hostname3=node2
zookeeper_dir=$1

# Checkout zk
for hostname in $hostname1 $hostname2 $hostname3
do
    ssh -t $user@$hostname "cd $zookeeper_dir ; git checkout tags/release-3.6.3"
done

# Change zoo.cfg based on param
sed -i "s dataDir=.* dataDir=$zookeeper_dir/data g" zoo.cfg
sed -i "s server.1=.* server.1=$hostname1:2888:3888 g" zoo.cfg
sed -i "s server.2=.* server.2=$hostname2:2888:3888 g" zoo.cfg
sed -i "s server.3=.* server.3=$hostname3:2888:3888 g" zoo.cfg

# Copy zoo.cfg to nodes
for hostname in $hostname1 $hostname2 $hostname3
do
    scp zoo.cfg $user@$hostname:$zookeeper_dir/conf/
done

# Create myid in nodes
ssh -t $user@$hostname1 "mkdir -p $zookeeper_dir/data ; cd $zookeeper_dir/data ; echo 1 > myid"
ssh -t $user@$hostname2 "mkdir -p $zookeeper_dir/data ; cd $zookeeper_dir/data ; echo 2 > myid"
ssh -t $user@$hostname3 "mkdir -p $zookeeper_dir/data ; cd $zookeeper_dir/data ; echo 3 > myid"

# Apply patch, install zk
for hostname in $hostname1 $hostname2
do
    scp follower.patch $user@$hostname:$zookeeper_dir/
    ssh -t $user@$hostname "cd $zookeeper_dir/ ; git apply follower.patch ; mvn install -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true"
done

scp leader.patch $user@$hostname3:$zookeeper_dir/
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; git apply leader.patch ; mvn install -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true"
