#! /bin/bash
# $1 is hbase dir

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )
cd "$1" || exit 1

./bin/start-hbase.sh
echo "create 't01', 'info', {REGION_REPLICATION => 65537} " | ./bin/hbase shell
echo "list TABLE " | ./bin/hbase shell
echo "create 't01', 'info'" | ./bin/hbase shell

echo "Last command will result in 'ERROR: Table already exists: t01!' even though the table doesn' exist as shown in 'list TABLE' command result "

./bin/stop-hbase.sh
