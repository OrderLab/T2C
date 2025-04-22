#! /bin/bash
# $1 is hbase dir

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1
./bin/start-hbase.sh

cd $script_dir/
cd ../client
./gradlew run --args=25827

cd "$1" || exit 1
./bin/stop-hbase.sh
