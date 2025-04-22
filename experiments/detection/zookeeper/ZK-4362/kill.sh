#! /bin/bash

cd "$1" || exit

echo "Stopping ZK server"
sudo ./bin/zkServer.sh stop