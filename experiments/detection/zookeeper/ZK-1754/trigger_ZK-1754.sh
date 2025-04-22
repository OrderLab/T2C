#! /bin/bash
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1

./bin/zkServer.sh start
sleep 2
./bin/zkServer.sh status

$script_dir/ZK-1754.sh
