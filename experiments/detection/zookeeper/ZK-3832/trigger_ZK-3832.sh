#! /bin/bash

cd "$1" || exit

sudo ./bin/zkServer.sh start || exit

(sleep 3; echo quit) | sudo ./bin/zkCli.sh -server 127.0.0.1:2183

echo -e "////////////////////////////////////////////////\n"

echo "java.lang.IllegalArgumentException: Invalid type: 1"
echo "This is caused by the bug where it can't detect any SAN that is not DNS or IP"

echo -e "\n////////////////////////////////////////////////\n"
