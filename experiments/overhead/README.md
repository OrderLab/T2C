# Overhead Evaluation

## Install JVMTop
```
wget https://github.com/patric-r/jvmtop/releases/download/0.8.0/jvmtop-0.8.0.tar.gz
tar -xzvf jvmtop-0.8.0.tar.gz
cd jvmtop-0.8.0.tar.gz
```

## Run target system
Run the target system, write down the pid number

You can get the pid value of zookeeper by running `cat $zookeeper_data/zookeeper_server.pid`, cassandra by running `pgrep -u "$USER" -f cassandra`. You need to get the pid value of hdfs and hbase manually.

## Record the cpu and memory usage
1. Modify the pid and out_file variable `run.sh`
2. Run the workload. You can use the throughput workload.
3. Run immediately after the workload starts.
```
./run.sh
```
4. Kill `run.sh` by using `CTRL+C` immediately after the workload has finished
5. Modify path_raw in `raw.py` using the same file as out_file in `run.sh`. Also modify the path variable in `raw.py` for the output. This python script will filter the output of the run.sh
```
python3 raw.py
```
6. Modify path in `logparser.py` using the same value as path in `raw.py`. This script will calculate the average of the cpu and memory usage
```
python3 logparser.py
```