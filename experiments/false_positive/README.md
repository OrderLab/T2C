# T2C False Positive Experiments

The false positive experiment is done using [Jepsen](https://github.com/jepsen-io/jepsen). We're using Jepsen's fault injection capability.

First, set this variable
```
rootdir_falsepos=`pwd`
```

## 0. Modify some parameters
Create a folder named lein_repo under the home (~) directory. Use the ABSOLUTE PATH
1. Modify `repo` variable in java-client/runner.sh with the abs path of the created lein_repo
2. Modify `:repositories` in jepsen/zookeeper/project.clj with the abs path of the created lein_repo
3. Modify jepsen/zookeeper/systarget/clj, change `(def privkey (str "/u/vqx2dc/.ssh/uva_ed"))` into the path of the 1-node's private key that can be used to ssh into the 5-nodes
4. Modify jepsen/zookeeper/systarget/clj, change vqx2dc in `(def username "vqx2dc")` into the username of the 5-nodes cluster
5. Modify jepsen/zookeeper/systarget/clj, change `(def dir (str "/localtmp/" systarget "/"))` into the zookeeper absolute path of the 5-nodes cluster. In this example, `systarget` value is "zookeeper", so `str "/localtmp/" systarget "/"` means the zookeeper dir is in "/localtmp/zookeeper/"
6. Modify java-client/src/main/resources/core-site.xml, hbase-site.xml, and hdfs-site.xml, change lift11-15 to your cluster hostnames. If the value is lift11, then replace it to the first node of your cluster

Do step 3-5 for the other systems

## 1. Java client
To interact with the target system, we need separate clients for each system. These clients are located in the `java-client`.

### 1.1 Compile the clients
```
cd $rootdir_falsepos/java-client
./runner.sh package
```
### 1.2 Deploy the client to a local maven repo
```
./runner.sh deploy
cd ..
```

## 2. Run the experiments
### 2.1 Ensuring the environment is ready
Jepsen is based on `clojure`, with package management `leiningen`. They need JDK version of more than 8. In this automation, we've provided the environment in a `nix flake` file that is guaranteed to run. Make sure you have installed `nix` and `direnv` as instructed in the parent folder.
```
cd $rootdir_falsepos/jepsen
# Make sure direnv has activated the nix flake. Run this command to check
lein --version #Something similar like this: Leiningen 2.11.2 on Java 21.0.3 OpenJDK 64-Bit Server VM
java -version #openjdk version "21.0.3"
```

### 2.2 Running the experiment
Prepare a `6 nodes` cluster. This cluster is divided into 2:
1. 1-node for running jepsen. This node will NOT run the target system
2. 5-nodes for running the target system. Make sure the 1-node is able to ssh to each of the 5-nodes using ssh key. Run the target system on these nodes.

Change the 5-nodes name and ssh private key. Run the following command on the 1-node.
```
cd $rootdir_falsepos/jepsen/zookeeper
lein run test --node lift11 --node lift12 --node lift13 --node lift14 --node lift15 --ssh-private-key ~/.ssh/uva_ed
```

#### 2.2.1 Zookeeper example
1. Make sure you have generated T2C checkers (do all steps from apply until validate) and logs is enabled
2. Change all nodes zookeeper config in `$zookeeper_dir/conf/zoo.conf` into
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

3. Create myid file in all zookeeper dataDir (in this example /users/dimas/zookeeper/data).
```
mkdir /users/dimas/zookeeper/data && echo 1 > myid
```
The number 1 depends on which node it isThe number 1 depends on which node it is. In our zoo.conf example, server.1 is node0, which means the myid in node0 is 1. Following this logic, myid of node1 is 2 and so on

4. Run zookeeper on each node
```
#run on each node
cd $zookeeper_dir
./bin/zkServer.sh start
```

5. Run experiment
```
cd $rootdir_falsepos/jepsen/zookeeper
lein run test --node lift11 --node lift12 --node lift13 --node lift14 --node lift15 --ssh-private-key ~/.ssh/uva_ed
```

Check the checker executions result in `$zookeeper_folder/t2c.prod.log` where $zookeeper_folder is the target system folder in each node.