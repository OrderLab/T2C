#! /bin/bash

cd "$1" || exit

if [ $# -eq 2 ]
  then
    sleep 120 || exit 1
fi

echo "Stopping ZK server"
sudo ./bin/zkServer.sh stop