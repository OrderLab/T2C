#! /bin/bash

cd "$1" || exit

echo "Stopping ZK server"
./bin/zkServer.sh stop
rm -rf version-2/