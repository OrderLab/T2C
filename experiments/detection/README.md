# Bug detection pool

> Run buggy case automatically
## 0. Explanation
In general, each automation script contains 3 scripts. Each case has their own readme for the detailed explanation.

1. Additional patch

An example of it is [hook_ZK-1754.sh](zookeeper/ZK-1754/hook_ZK-1754.patch). This patch is used to help us reproducing the failure. When we're trying to detect the bug using T2C, we need to apply both this patch and the T2C hook patch.

2. Installation script `install_ZK-1754.sh`

>Don't use this script when trying to use T2C.

An example of it is [install_ZK-1754.sh](zookeeper/ZK-1754/install_ZK-1754.sh). This script is used to prepare (apply patch+compile) the target system. When we're trying to detect the bug using T2C, we don't need to run this script because it's handled by the T2C script (we still need to apply the additional patch).

3. Trigger script

An example of it is [trigger_ZK-1754.sh](zookeeper/ZK-1754/trigger_ZK-1754.sh). This script will start the target system and trigger the failure.

4. Cleanup script

An example of it is [kill.sh](zookeeper/ZK-1754/kill.sh). This script will stop the target system and delete the existing data.

5. All script

>Don't use this script when trying to use T2C.

Some automation script will include the all script that automatically run install script, trigger script, and cleanup script. 

## 1. How to run
### 1.1 Zookeeper
#### 1.1.1 ZK-1754
Refer to [ZK-1754 guide](zookeeper/ZK-1754/README.md)
#### 1.1.2 ZK-1208
Refer to [ZK-1208 guide](zookeeper/ZK-1208/README.md)
#### 1.1.3 ZK-2355
Refer to [ZK-2355 guide](zookeeper/ZK-2355/README.md)
#### 1.1.4 ZK-4026
Refer to [ZK-4026 guide](zookeeper/ZK-4026/README.md)
#### 1.1.5 ZK-4325
Refer to [ZK-4325 guide](zookeeper/ZK-4325/README.md)
#### 1.1.6 ZK-4362
Refer to [ZK-4362 guide](zookeeper/ZK-4362/README.md)
#### 1.1.7 ZK-4646
Refer to [ZK-4646 guide](zookeeper/ZK-4646/README.md)

### 1.2 Cassandra
#### 1.2.1 CS-14092
Refer to [CS-14092 guide](cassandra/CASSANDRA-14092/README.md)
#### 1.2.2 CS-14803
Refer to [CS-14803 guide](cassandra/CASSANDRA-14803/README.md)
#### 1.2.3 CS-14873
Refer to [CS-14873 guide](cassandra/CASSANDRA-14873/README.md)
#### 1.2.4 CS-15072
Refer to [CS-15072 guide](cassandra/CASSANDRA-15072/README.md)

### 1.3 HDFS
#### 1.3.1 HDFS-14699
Refer to [HDFS-14699 guide](hdfs/HDFS14699/README.md)
#### 1.3.2 HDFS-16942
Refer to [HDFS-16942 guide](hdfs/HDFS16942/README.md)
#### 1.3.3 HDFS-16633
Refer to [HDFS-16633 guide](hdfs/HDFS16633/README.md)
#### 1.3.4 HDFS-14514
Refer to [HDFS-14514 guide](hdfs/HDFS14514/README.md)

### 1.4 HBase
#### 1.4.1 HBase-21644
Refer to [HB-21644 guide](hbase/HBASE-21644/README.md)
#### 1.4.2 HBase-21041
Refer to [HB-21041 guide](hbase/HBASE-21041/README.md)
#### 1.4.3 HBase-21621
Refer to [HB-21621 guide](hbase/HBASE-21621/README.md)
#### 1.4.4 HBase-25827
Refer to [HB-25827 guide](hbase/HBASE-25827/README.md)
#### 1.4.5 HBase-28481
Refer to [HB-28481 guide](hbase/HBASE-28481/README.md)
