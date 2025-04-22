# T2C False Positive Experiments

The false positive experiment is done using [Jepsen](https://github.com/jepsen-io/jepsen). We're using Jepsen's fault injection capability.

First, set this variable
```
rootdir_falsepos=`pwd`
```

## 1. Java client
To interact with the target system, we need separate clients for each system. These clients are located in the `java-client`.

### 1.1 Compile the clients
Modify the runner.sh `repo` variable to a folder.
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

For each jepsen script (for example, jepsen/zookeeper). Modify `:repositories` in project.clj to the local folder used as the variable `repo` in section 1.1

### 2.2 Running the experiment
Prepare a `6 nodes` cluster. This cluster is divided into 2:
1. 1-node for running jepsen. This node will NOT run the target system
2. 5-nodes for running the target system. Make sure the 1-node is able to ssh to each of the 5-nodes using ssh key. Run the target system on these nodes.

Change the 5-nodes name and ssh private key. Run the following command on the 1-node.
```
cd $rootdir_falsepos/jepsen/zookeeper
lein run test --node lift11 --node lift12 --node lift13 --node lift14 --node lift15 --ssh-private-key ~/.ssh/uva_ed
```

Check the checker executions result in `$zookeeper_folder/t2c.prod.log` where $zookeeper_folder is the target system folder in each node.