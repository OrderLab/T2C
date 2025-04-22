#! /bin/bash

cd "$1" || exit

sudo ./bin/zkServer.sh start || exit

(sleep 3; echo quit) | sudo ./bin/zkCli.sh -server 127.0.0.1:2183

echo -e "\n////////////////////////////////////////////////\n"

echo "Notice that client connected successfully even though it should not connect"

echo -e "\n////////////////////////////////////////////////\n"
