# Zookeeper throughput eval

1. Build the benchmark

```bash
go mod tidy
go build # You'll have a binary of zkbench
```

2. Change bench_perf.conf according to your cluster config

3.  Checkout Zookeeper version and compile (~3 min)

```bash
cd zookeeper
git stash && git checkout release-3.6.1
mvn clean package -DskipTests
```

### Start workload benchmark (~3 min)

Run on all nodes (the script would automatically start the instances):

```bash
./throughput.sh <zk absolute dir> <zkbench absolute dir> <master node hostname>
```

### Collect results

For performance, see logs in `~/go/src/zkbench/zkresult-[date]-summary.dat`, example:

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
