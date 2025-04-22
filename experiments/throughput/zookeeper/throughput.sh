#run this scripts on all nodes
# sudo ./throughput.sh <zk absolute dir> <zkbench absolute dir> <master node hostname>

cd $1
./bin/zkServer.sh stop
sleep 2

rm t2c.prod.log
./bin/zkServer.sh start

sleep 60

echo "quit" | ./bin/zkCli.sh

sleep 10
./bin/zkServer.sh status

sleep 2

if [ "$(hostname -s)" = $3 ]; then
    cd $2
    ./zkbench -conf bench_perf.conf
fi

