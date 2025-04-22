#run this on single node
#usage: trigger_ZK-1754.sh [T2C_root] [zk_root]
cd $2
bin/zkServer.sh stop
rm -rf /tmp/zookeeper/version-2
sleep 2

rm conf/zoo.cfg.1754
cp conf/zoo_sample.cfg conf/zoo.cfg.1754

rm /tmp/zookeeper/myid
echo "1" > /tmp/zookeeper/myid

echo "server.0=localhost:2110:3110" >> conf/zoo.cfg.1754
echo "server.1=localhostfake1:2110:3110" >> conf/zoo.cfg.1754
echo "server.2=localhostfake2:2110:3110" >> conf/zoo.cfg.1754
echo "server.3=localhostfake3:2110:3110" >> conf/zoo.cfg.1754
echo "server.4=localhostfake4:2110:3110" >> conf/zoo.cfg.1754

bin/zkServer.sh start zoo.cfg.1754
sleep 2
bin/zkServer.sh status
sleep 2
bin/zkCli.sh -r <<EOF
  multi
EOF

bin/zkCli.sh -r <<EOF
  ls /
EOF

echo "you should see create node /bad successful, which means read-only semantics is violated"

