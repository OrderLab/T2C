#! /bin/bash

cd "$1" || exit

sudo ./bin/zkServer.sh start || exit

(echo "create /a"; echo "quit") | sudo ./bin/zkCli.sh -server 127.0.0.1:2181

echo -e "////////////////////////////////////////////////\n"

echo "Notice that equal assertions return multiple falses, not null assertions return multiple null"

echo -e "\n////////////////////////////////////////////////\n"