# Source code repository for the T2C project

## Overview

T2C (test-to-checker) is a system tool to synthesize runtime checkers from existing test
cases to detect silent semantic violations in production systems. 

It works in two phases. In the offline phase, it provides a static analyzer
to extract test assertions, and a dynamic framework to schedule test runs
under instrumented systems and record pre-conditions. The output of 
offline phase is a set of runtime checker templates and a pool of 
extracted assertions to be executed later. In the online phase, it
provides plugin checker components to load templates and assertions
and activate them to detect failures. It also provides scripts to install
such components to target systems so they can be deployed together.


Table of Contents
================
- [Source code repository for the T2C project](#source-code-repository-for-the-t2c-project)
   * [Overview](#overview)
   * [Requirements](#requirements)
   * [Getting Started Instructions](#getting-started-instructions)
      + [0. [Pre] Install dependencies](#0-pre-install-dependencies)
      + [1. [Pre] Clone the T2C repository](#1-pre-clone-the-t2c-repository)
      + [2. [Pre] Build T2C (~3 min)](#2-pre-build-t2c-3-min)
      + [3. [Pre] Customize configurations (~1 min)](#3-pre-customize-configurations-1-min)
      + [4. [Pre] Clone, patch and build the target system (~5 min)](#4-pre-clone-patch-and-build-the-target-system-5-min)
         - [4.1 Clone the target system](#41-clone-the-target-system)
         - [4.2 Patch systems with T2C ](#42-patch-systems-with-t2c)
         - [4.3 Build systems with T2C](#43-build-systems-with-t2c)
      + [5. [Offline] Retrofit test case classes (~3 min)](#5-offline-retrofit-test-case-classes-3-min)
      + [6. [Offline] Execute tests and generate checker templates (~45min on 5 nodes)](#6-offline-execute-tests-and-generate-checker-templates-45min-on-5-nodes)
      + [7. [Offline] Validate generated checker templates (~45min on 5 nodes)](#7-offline-validate-generated-checker-templates-45min-on-5-nodes)
      + [8. [Online] Runtime Detection (~15 min)](#8-online-runtime-detection-15-min)
         - [8.1 Load templates and assertion pool](#81-load-templates-and-assertion-pool)
         - [8.2 Run target system](#82-run-target-system)
            * [8.2.1 Zookeeper](#821-zookeeper)
            * [8.2.2 Cassandra](#822-cassandra)
            * [8.2.3 HDFS](#823-hdfs)
            * [8.2.4 HBase](#824-hbase)
         - [8.3 Check detection results](#83-check-detection-results)
      + [9. Evaluation](#9-evaluation)


## Requirements

* OS and JDK:
    - T2C is developed and tested under **Ubuntu 18.04/20.04** and **JDK 8**.
    - Other systems and newer JDKs may also work. We tested a few functionalities 
      on macOS Catalina (10.15.7) and Ventura (13.5.2) but the correctness is not guaranteed.

* Git (>= 2.16.2, version control)
* Apache Maven (>= 3.6.3, for T2C compilation)
* Apache Ant (>= 1.10.9, artifact testing only, for zookeeper compilation)
* JDK8 (openjdk recommended)

## Getting Started Instructions


### 0. [Pre] Install dependencies


```bash
sudo apt-get update
sudo apt-get install -y git maven ant vim openjdk-8-jdk golang-go gnuplot
```

Make sure you set JDK to be openjdk-8. You should also set the `JAVA_HOME`
environment variable properly (and add it to `.bashrc`):

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
echo export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 >> ~/.bashrc
```

Check whether Java 8 is successfully set by running `java -version`. If it's not set then you might want to add it manually to the PATH by running
```bash
export PATH="/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH"
echo export PATH="/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH" >> ~/.bashrc
```

### 1. [Pre] Clone the T2C repository

To clone from github:

```bash
git clone https://github.com/OrderLab/T2C.git
```


### 2. [Pre] Build T2C (~3 min)

T2C uses Maven for project management.

To compile and run tests:

```bash
cd T2C && ./run_engine.sh compile
```

### 3. [Pre] Customize configurations (~1 min)

A configuration is needed to specify the basic information about the target system.
We have already prepared a list of configurations for common
systems. All you need to do is to customize it based on your
own environment.

```
vim conf/samples/zk-3.4.11.properties
```
A sample config file looks like:

```properties
#Required (user-specific):
system_dir_path=/Users/McfateAlan/zookeeper/
patch_path=${t2c_dir}/conf/samples/zk-patches/install_zk-3.4.11.patch
ticket_collection_path=${t2c_dir}/conf/samples/zk-collections

#Required (system related):
commit_id=release-3.4.11
java_class_path="${system_dir_path}/build/classes/:${system_dir_path}/build/lib/*"
#java_class_path="{system_dir_path}/build/test/classes/:{system_dir_path}/lib/*:{system_dir_path}/build/classes/:{system_dir_path}/build/lib/*:{system_dir_path}/build/test/lib/*"
system_classes_dir_path="${system_dir_path}/build/classes/"
test_classes_dir_path="${system_dir_path}/build/test/classes/"
runtime_lib_path="${system_dir_path}/build/lib/"
install_lib_cmd="(mkdir -p ${runtime_lib_path} && cp ${t2c_dir}/target/*.jar ${runtime_lib_path})"
client_main_class=org.apache.zookeeper.ZooKeeperMain
system_package_prefix=org.apache.zookeeper
test_class_name_regex=.*Test$
compile_test_cmd="ant compile-test"
clean_test_cmd="ant clean"
runtime_bench_cmd="bin/zkServer.sh stop && bin/zkServer.sh start && bin/zkServer.sh status && sleep 2 && bin/zkCli.sh << EOF && cat t2c.prod.log"
jvm_args_for_tests=
...
```

You only need to update `system_dir_path` with the path you plan to install
the target system.

### 4. [Pre] Clone, patch and build the target system (~5 min)

#### 4.1 Clone the target system

Clone the Git repository for the target system. We
use [ZooKeeper](https://github.com/apache/zookeeper) as an example:

```bash
git clone git@github.com:apache/zookeeper.git
cd zookeeper && git checkout tags/release-3.4.11
```

Similarly for other three systems we included in the evaluation:

```bash
git clone git@github.com:apache/cassandra.git
git clone git@github.com:apache/hadoop.git
git clone git@github.com:apache/hbase.git

cd cassandra && git checkout tags/cassandra-3.11.5
cd hadoop && git checkout tags/rel/release-3.2.2
cd hbase && git checkout tags/rel/2.4.0
```

#### 4.2 Patch systems with T2C 

```bash
./run_engine.sh patch conf/samples/zk-3.4.11.properties zookeeper
```

#### 4.3 Build systems with T2C

```bash
./run_engine.sh recover_tests conf/samples/zk-3.4.11.properties
```

This step will do a compilation on the target system.
It can also revert all modifications made in the retrofit
phase and revert the system classes to a clean compiled state,
which is very useful if you encounter errors in the retrofit 
stage.


### 5. [Offline] Retrofit test case classes (~3 min)

```bash
./run_engine.sh retrofit conf/samples/zk-3.4.11.properties 
```

if you encounter error:

```dtd
assertpacker.mark exists. Already transformed before, exit
If you think this is a mistake, you should force to clean the marker by delete it
```

this means the class files have already been modified. 
You can run `recover_tests` command mentioned in the earlier 
section to revert the .

### 6. [Offline] Execute tests and generate checker templates (~45min on 5 nodes)

```bash
# You might want to run this command in tmux to prevent it being cancelled accidentally
./run_engine.sh build conf/samples/zk-3.4.11.properties 
```

Generated output would be saved in the target system, for example, zookeeper will save it in `zookeeper/templates_out`

> [!TIP]  
> T2C also supports `parallel mode` (partition all test execution to distributed nodes):
> 
> * create a new file or edit existing file in conf/workers, each line should be the hostname of node you want to use. Here is an example of the config file:
> > [!IMPORTANT]  
> > The file *must* end with a new line.
> ```
> lift11
> lift12
> lift13
> lift14
> lift15
> ```
> * then replace your build command with: `./run_engine.sh parallel_build conf/samples/zk-3.4.11.properties conf/workers`
> You only need to run this in one of node, it will automatically create new processes in the all nodes to run the process at background.

### 7. [Offline] Validate generated checker templates (~45min on 5 nodes)

First, to validate the generated checkers, they need to be 
moved to a directory in the target system, for example, in zookeeper we need the directory  `zookeeper/templates_in`

```bash
cd <system path>
mv templates_out templates_in
# for HDFS, also run this command
# cp -r <system_path>/templates_out ~/templates_in
```

then start to validate with:

```bash
# You might want to run this command in tmux to prevent it being cancelled accidentally
./run_engine.sh validate conf/samples/zk-3.4.11.properties 
```

generated results would be saved in `T2C/inv_verify_output`

> [!TIP]  
> Similarly, if you use `parallel mode`:  
> * copy the templates folder to all nodes
> ```
> # run all 5 on all 5 compute nodes
> mkdir <system_path>/templates_in
>
> rsync -Pavz <system_path>/templates_out/* <user>@<lift11>:<system_path>/templates_in
>
> rsync -Pavz <system_path>/templates_out/* <user>@<lift12>:<system_path>/templates_in
>
> rsync -Pavz <system_path>/templates_out/* <user>@<lift13>:<system_path>/templates_in
>
> rsync -Pavz <system_path>/templates_out/* <user>@<lift14>:<system_path>/templates_in
>
> rsync -Pavz <system_path>/templates_out/* <user>@<lift15>:<system_path>/templates_in
> ```
> * run the validate phase
> ```
> ./run_engine.sh parallel_validate conf/samples/zk-3.4.11.properties conf/workers
> ```

### 8. [Online] Runtime Detection (~15 min)

#### 8.1 Load templates and assertion pool

> For HDFS, put the templates folder in `~`. The end result should be `~/templates_in/`

Delete templates_in folder if it exists, then move the validated checkers from `inv_verify_output/verified_inv_dir/` in T2C to `templates_in` in the target system,
for example:

```bash
cp -r <T2C>/inv_verify_output/verified_inv_dir/ <system path>/templates_in/
```

Note that the templates_in folder should only contain the templates from inv_verify_output/verified_inv_dir/.

> [!TIP]  
> Similarly, if you use `parallel mode`:  
> * copy the result to all nodes
> ```
> # run all 5 on all 5 compute nodes
> mv <system_path>/templates_in <system_path>/templates_in_bak
> mkdir <system_path>/templates_in
>
> rsync -Pavz <T2C>/inv_verify_output/verified_inv_dir/* <user>@<lift11>:<system_path>/templates_in
>
> rsync -Pavz <T2C>/inv_verify_output/verified_inv_dir/* <user>@<lift12>:<system_path>/templates_in
>
> rsync -Pavz <T2C>/inv_verify_output/verified_inv_dir/* <user>@<lift13>:<system_path>/templates_in
>
> rsync -Pavz <T2C>/inv_verify_output/verified_inv_dir/* <user>@<lift14>:<system_path>/templates_in
>
> rsync -Pavz <T2C>/inv_verify_output/verified_inv_dir/* <user>@<lift15>:<system_path>/templates_in
> ```

#### 8.2 Run target system
> If you're trying to detect bug cases that we provide in [https://github.com/OrderLab/T2C-EvalAutomatons/tree/master/detection](https://github.com/OrderLab/T2C-EvalAutomatons/tree/master/detection), you should follow the instruction for each bug because some bugs might require additional patching. The instruction here is specific for general runtime detection without a specific bug case in mind.

##### 8.2.1 Zookeeper
```
./bin/zkServer.sh start
```
##### 8.2.2 Cassandra
```
./bin/cassandra
```
##### 8.2.3 HDFS
```
./bin/hdfs namenode -format
./sbin/start-dfs.sh
```
##### 8.2.4 HBase
```
./bin/start-hbase.sh
```

#### 8.3 Check detection results

The checking result will be printed to stdout (or redirected to logs depending on target system's log configuration, for example, zookeeper saves output to `t2c.prod.log`). If some invariant fails and report, the log would print failed invariants such as:
```
SuccessMap: {org.apache.zookeeper.test.QuorumQuotaTest#testQuotaWithQuorum-270=396, org.apache.zookeeper.test.QuorumQuotaTest#testQuotaWithQuorum-271=393, org.apache.zookeeper.server.ZooKeeperServerMainTest#testJMXRegistrationWithNIO-374=2, org.apache.zookeeper.test.ZooKeeperQuotaTest#testQuota-359=393, org.apache.zookeeper.test.ClientPortBindTest#testBindByAddress-438=1, org.apache.zookeeper.test.GetChildren2Test#testChild-469=385, org.apache.zookeeper.ZooKeeperTest#testDeleteRecursiveAsync-191=387, org.apache.zookeeper.test.GetChildren2Test#testChild-471=382, org.apache.zookeeper.test.StatTest#testChildren-19=2526, org.apache.zookeeper.test.GetChildren2Test#testChild-473=388, org.apache.zookeeper.test.StatTest#testChildren-17=2540, org.apache.zookeeper.test.WatcherFuncTest#testExistsSync-455=175, org.apache.zookeeper.test.StatTest#testChildren-15=2535, org.apache.zookeeper.test.WatcherFuncTest#testExistsSync-454=175, org.apache.zookeeper.server.ZooKeeperServerMainTest#testStandalone-372=1, org.apache.zookeeper.test.WatcherTest#testWatcherAutoResetDisabledWithLocal-54=309, org.apache.zookeeper.test.GetChildren2Test#testChildren-478=2548, org.apache.zookeeper.server.quorum.auth.QuorumDigestAuthTest#testRelectionWithValidCredentials-31=1, org.apache.zookeeper.test.GetChildren2Test#testChildren-476=2581, org.apache.zookeeper.ZooKeeperTest#testDeleteRecursive-185=392, org.apache.zookeeper.test.GetChildren2Test#testChildren-474=2612, org.apache.zookeeper.ZooKeeperTest#testDeleteRecursive-186=387, org.apache.zookeeper.ZooKeeperTest#testDeleteRecursive-187=396, org.apache.zookeeper.server.ZooKeeperServerMainTest#testJMXRegistrationWithNetty-374=1, org.apache.zookeeper.test.StatTest#testDataSizeChange-24=380, org.apache.zookeeper.test.StatTest#testDataSizeChange-23=384, org.apache.zookeeper.test.QuorumQuotaTest#testQuotaWithQuorum-268=391, org.apache.zookeeper.test.QuorumQuotaTest#testQuotaWithQuorum-269=388, org.apache.zookeeper.test.StatTest#testDataSizeChange-22=389, org.apache.zookeeper.test.StatTest#testDataSizeChange-21=392, org.apache.zookeeper.test.StatTest#testDataSizeChange-20=389, org.apache.zookeeper.test.MaxCnxnsTest#testMaxCnxns-616=389, org.apache.zookeeper.test.StatTest#testBasic-5=385, org.apache.zookeeper.test.QuorumZxidSyncTest#testLateLogs-101=389, org.apache.zookeeper.test.StatTest#testBasic-9=392, org.apache.zookeeper.test.StatTest#testBasic-8=393, org.apache.zookeeper.test.StatTest#testBasic-7=388, org.apache.zookeeper.test.StatTest#testBasic-6=387, org.apache.zookeeper.server.ZooKeeperServerMainTest#testNonRecoverableError-370=1, org.apache.zookeeper.test.SessionInvalidationTest#testCreateAfterCloseShouldFail-262=389, org.apache.zookeeper.server.SessionTrackerTest#testCloseSessionRequestAfterSessionExpiry-236=1, org.apache.zookeeper.server.PrepRequestProcessorTest#testMultiOutstandingChange-109=218, org.apache.zookeeper.server.PrepRequestProcessorTest#testMultiOutstandingChange-108=218, org.apache.zookeeper.test.SaslAuthFailNotifyTest#testBadSaslAuthNotifiesWatch-479=398, org.apache.zookeeper.ZooKeeperTest#testDeleteRecursiveAsync-189=400, org.apache.zookeeper.test.ZooKeeperQuotaTest#testQuota-362=390, org.apache.zookeeper.test.ZooKeeperQuotaTest#testQuota-361=388, org.apache.zookeeper.test.ZooKeeperQuotaTest#testQuota-360=387, org.apache.zookeeper.test.WatcherTest#testWatcherAutoResetDisabledWithGlobal-54=333, org.apache.zookeeper.test.StatTest#testChild-14=382, org.apache.zookeeper.ZooKeeperTest#testDeleteRecursiveAsync-190=396, org.apache.zookeeper.test.StatTest#testChild-12=379, org.apache.zookeeper.test.StatTest#testChild-10=389, org.apache.zookeeper.server.ZooKeeperServerMainTest#testAutoCreateDataLogDir-373=1, org.apache.zookeeper.test.ChrootTest#testChrootSynchronous-390=394, org.apache.zookeeper.test.SaslAuthDesignatedServerTest#testAuth-330=388}
FailMap: {org.apache.zookeeper.test.SaslAuthDesignatedClientTest#testSaslConfig-367=402, org.apache.zookeeper.server.InvalidSnapCountTest#testInvalidSnapCount-828=1}
```

### 9. Evaluation
Evaluation cases readme are in this experiments [README](/experiments/README.md). This readme will guide you to install additional tools required to run the experiments. For these evaluations, we recommend starting from detecting ZK-1208 and HDFS-14699, then continue with false positive, and then ends with throughput and overhead. For false positive, throughput, and overhead baseline experiments, you can do it by running the system on the same version but without applying T2C on it.
1. [experiments/detection](/experiments/detection/README.md) folder contains the bug detection evaluation
2. [experiments/false_positive](/experiments/false_positive/README.md) folder contains the false positive evaluation
3. [experiments/throughput](/experiments/throughput/README.md) folder contains the throughput evaluation
4. [experiments/overhead](/experiments/overhead/README.md) folder contains the overhead evaluation
