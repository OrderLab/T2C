This is a brief instruction for HBase throughput evaluation in a 5-node cluster.

### Prerequisites

#### JDK Version

`java-8-openjdk-amd64` is used in this experiment. Other versions of java may also work, but you need to change configuration of `JAVA_HOME` in `hbase-env.sh` and  `hadoop-env.sh`

#### SSH configuration

The experiment requires that your nodes can ssh connect to each other without root permission. If not, please generate ssh keys **in each node**:

```shell
ssh-keygen -t rsa
```

Print the pub key:

```shell
cat ~/.ssh/id_rsa.pub
```

Copy the pub key and add it to all nodes’ `~/.ssh/authorized_keys`, including the node itself.

#### Protobuf

Run the script `build_protoc.sh` . After running this script, there should be a folder `protoc` at the same directory as `build_protoc.sh` . Add to `PATH`:  

```shell
export PROTOC_HOME=/path_to_protoc/protoc/2.5.0
export HADOOP_PROTOC_PATH=$PROTOC_HOME/dist/bin/protoc
export PATH=$PROTOC_HOME/dist/bin/:$PATH
source ~/.bashrc
```

### Modify Configuration Files

#### `regionservers`

Enter the hostnames of your nodes.

```
node1
node2
node3
node4
node5
```

Choose one node to be the master node.

#### `hbase-site.xml`

Replace `node1` with the hostname of your master node. 

Change the `username` in `hbase.zookeeper.property.dataDir` with your own username.

```xml
<configuration>
  <property>
    <name>hbase.zookeeper.quorum</name>
    <value>node1</value>
    <description>The directory shared by RegionServers.
    </description>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/home/username/hbase-2.4.0/dataDir</value>
    <description>Property from ZooKeeper config zoo.cfg.
    The directory where the snapshot is stored.
    </description>
  </property>
  <property>
    <name>hbase.rootdir</name>
    <value>hdfs://node1:8020/hbase</value>
    <description>The directory shared by RegionServers.
    </description>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
    <description>The mode the cluster will be in. Possible values are
      false: standalone and pseudo-distributed setups with managed ZooKeeper
      true: fully-distributed with unmanaged ZooKeeper Quorum (see hbase-env.sh)
    </description>
  </property>
</configuration>
```

#### `core-site.xml`

Replace `node1` with the hostname of your master node.

```xml
<configuration>
        <property>
                <name>fs.defaultFS</name>
                <value>hdfs://node1:8020</value>
        </property>
</configuration>
```

### Install

Run `install_master_node.sh` **in mater node**.

``` shell
./install_master_node.sh
```

Then run `install_other_node.sh` **in the other 4 nodes**.

```shell
./install_other_node.sh
```

### Start Experiments

Run `throughput.sh` **only in master node**.

```shell
./throughput.sh
```

> Please note that there are some prompts starting with `####` in terminal after running `throughput.sh`, which tell you where to find the results.
>
> The error message `ERROR NoMethodError: private method 'exit' called for nil:NilClass` in the output can be ignored.

Then check the results in terminal and `~/YCSB`.

### Clean up and Repeat

Run `cleanup.sh` **only in master node**.

```shell
./cleanup.sh
```

Then repeat `Start Experiments` section.

When you finish all the experiments, run 

```shell
/localtmp/hdfs/hadoop-dist/target/hadoop-3.2.2/sbin/stop-dfs.sh
```

 this will stop Hadoop service.