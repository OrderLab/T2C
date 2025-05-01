# Zookeeper throughput eval

1. Build the benchmark

```bash
go mod tidy
go build # You'll have a binary of zkbench
```

2. Change bench_perf.conf according to your cluster config

3.  Make sure you have generated T2C checkers (do all steps from apply until validate) and disable T2C logs

For baseline, you can use a fresh zookeeper 3.4.11 by running this command
```bash
cd zookeeper
git stash && git checkout release-3.4.11
ant
```

4. Change all nodes zookeeper config in `$zookeeper_dir/conf/zoo.conf` into
```
tickTime=2000
dataDir=/users/dimas/zookeeper/data
clientPort=2181
initLimit=5
syncLimit=2
server.1=node0:2888:3888
server.2=node1:2888:3888
server.3=node2:2888:3888
server.4=node3:2888:3888
server.5=node4:2888:3888
```
Make sure you change `/users/dimas/zookeeper` into your zookeeper folder, and change node0-5 into your hostname

5. Create myid file in all zookeeper dataDir (in this example /users/dimas/zookeeper/data).
```
mkdir /users/dimas/zookeeper/data && echo 1 > myid
```
The number 1 depends on which node it isThe number 1 depends on which node it is. In our zoo.conf example, server.1 is node0, which means the myid in node0 is 1. Following this logic, myid of node1 is 2 and so on

### Start workload benchmark (~3 min)

Run on all nodes (the script would automatically start the instances):

```bash
./throughput.sh <zk absolute dir> <zkbench absolute dir> <master node hostname>
```

### Collect results

For performance, see logs in `./zkbench/zkresult-[date]-summary.dat`, example:

```
1,MIXED,5,3000,0,3191706,181254,11187329,977819,9.575119909s,313.312003,2022-04-14T00:25:45.418795Z,1392:200:198:200:194:200:203:200:198:15
```

`313.312003` is the throughput on this client.

For active ratio, see logs in `zookeeper/logs/zookeeper-*.out`, for example:

```
Checking finished, succCount:.. failCount: .. inactiveCount: ..
```

Active ratio = 1-inactiveCount/(succCount+failCount+inactiveCount)

### Clean up

```bash
cd zookeeper
./bin/zkServer.sh stop
rm -r /tmp/zookeeper/version-2/
```

### Repeat for baseline result

Repeat above steps but do not install T2C and rules
