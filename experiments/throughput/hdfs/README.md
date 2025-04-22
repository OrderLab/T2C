# HDFS Throughput Eval

1. We assume you already configured in HDFS-14699 and will reuse the scripts and config in HDFS-14699.

2. Prepare
```bash
HADOOP_DIR= # CHANGE IT
export PDSH_RCMD_TYPE=ssh
export HADOOP_HOME=$HADOOP_DIR/hadoop-dist/target/hadoop-3.2.2
```

3. Cleanup. Run on all nodes
```bash
./cleanup.sh ~/hadoop lift11 # CHANGE THE TMP FOLDER. USUALLY hadoop-username
rm $HADOOP_DIR/hadoop/hadoop-dist/target/hadoop-3.2.2/logs/*
sudo rm -rf /tmp/hdfs /tmp/hadoop-dimas # CHECK YOUR TMP FOLDER AND DELETE IT. USUALLY hadoop-username
```

4. Run throughput. Run on namenode only (1 node)
```bash
./throughput.sh ~/hadoop
```

The result would be printed out in the terminal like:

```
Job started: 0
Job ended: ...
The createWrite job took 90 seconds.
The job recorded 0 exceptions.
```

If so, the throughput is 16000/90=177.77 op/s.

(Sometimes you might encounter benchmark fails to connect, retry cleaning and benchmark usually resolves the problem.)

(If you encounter issues of low resource exceptions and HDFS cluster is forced to enter safe mode, try to reduce benchmark size in the file `OathKeeper/experiments/run/hdfs/throughput.sh` or provide more resources.)

5. Cleanup again
6. Repeat