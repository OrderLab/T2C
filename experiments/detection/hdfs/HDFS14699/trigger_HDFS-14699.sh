#! /bin/bash
#run this scripts on namenode

# $1 is hdfs dir
# $2 $3 $4 is datanode hostname/ip

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1
version='3.2.1'

./bin/hdfs namenode -format orderlab
./sbin/start-dfs.sh

cd hadoop-dist/target/hadoop-${version}/
$script_dir/HDFS-14699.sh ${version} $1 $2 $3 $4 # version, dir, host1, host2, host3

