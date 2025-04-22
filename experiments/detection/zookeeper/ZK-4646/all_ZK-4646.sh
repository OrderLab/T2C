#! /bin/bash

zookeeper_dir=/users/dimas/zookeeper # NO TRAILING SLASH

./install_ZK-4646.sh $zookeeper_dir || exit
./trigger_ZK-4646.sh $zookeeper_dir
./kill_ZK-4646.sh $zookeeper_dir || exit