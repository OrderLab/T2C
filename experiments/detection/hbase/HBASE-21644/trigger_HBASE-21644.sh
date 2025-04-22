#! /bin/bash
# $1 is hbase dir

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )
cd "$1" || exit 1
 
./bin/start-hbase.sh
echo -e "\e[34mbuild an example table\e[0m"
echo "create 'tableName', {NAME => 'cf'}, {REGION_REPLICATION => 5}" | ./bin/hbase shell
echo -e "\e[34mNext change the max size of file size, and this will be in infinite loop, this is where the bug appears. If patch is applied, this will success otherwise.\e[0m"
echo "alter 'tableName', {NAME => 'cf', METHOD => 'table_att', MAX_FILESIZE => '1073741824'}" | ./bin/hbase shell
./bin/stop-hbase.sh
