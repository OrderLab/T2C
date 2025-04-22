#! /bin/bash

cd "$1" || exit

sudo ./bin/zkServer.sh start || exit

(echo "create /a"; sleep 30; echo "create /b";  sleep 30; echo "create /c"; echo quit) | sudo ./bin/zkCli.sh -server 127.0.0.1:2181

echo -e "////////////////////////////////////////////////\n"

echo "ls result: "
ls version-2/
echo "Check that there are 3 snapshots"
echo "snapshot.2 is generated because of the bug"

echo -e "\n////////////////////////////////////////////////\n"