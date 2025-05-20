# CS-15072

## Without T2C
```
./all_CASSANDRA-15072.sh <cassandra_absolute_path>
```

## With T2C
### 1. Preparation
#### 1.1 Prepare a 3-nodes cluster
In the node 0, clone cassandra 2.1.17
```
cd ~

git clone https://github.com/apache/cassandra.git cs2117
cd ~/cs2117 && git checkout tags/cassandra-2.1.17
sed -i "s|http://repo2.maven.org|https://repo1.maven.org|g" ~/cs2117/build.xml
sed -i "s|http://|https://|g" ~/cs2117/build.properties.default
```

In the node 1, clone cassandra 2.1.17 and 3.11.4
```
cd ~

git clone https://github.com/apache/cassandra.git cs2117
cd ~/cs2117 && git checkout tags/cassandra-2.1.17
sed -i "s|http://repo2.maven.org|https://repo1.maven.org|g" ~/cs2117/build.xml
sed -i "s|http://|https://|g" ~/cs2117/build.properties.default

git clone https://github.com/apache/cassandra.git cs3114
cd ~/cs3114 && git checkout tags/cassandra-3.11.4
sed -i "s|http://repo2.maven.org|https://repo1.maven.org|g" ~/cs3114/build.xml
sed -i "s|http://|https://|g" ~/cs3114/build.properties.default
```

In the node 2, clone cassandra 2.1.17 and 3.11.4
```
cd ~

git clone https://github.com/apache/cassandra.git cs2117
cd ~/cs2117 && git checkout tags/cassandra-2.1.17
sed -i "s|http://repo2.maven.org|https://repo1.maven.org|g" ~/cs2117/build.xml
sed -i "s|http://|https://|g" ~/cs2117/build.properties.default

git clone https://github.com/apache/cassandra.git cs3114
cd ~/cs3114 && git checkout tags/cassandra-3.11.4
sed -i "s|http://repo2.maven.org|https://repo1.maven.org|g" ~/cs3114/build.xml
sed -i "s|http://|https://|g" ~/cs3114/build.properties.default
```
#### 1.2. Modify t2c config (RUN ON NODE 0 and 1)
In <t2c_dir>/conf/samples/cs-3.11.5.properties, modify `system_dir_path` to the cassandra folder absolute path, and modify `patch_path` to `${t2c_dir}/conf/samples/patches/install_cassandra-3.11.patch`

#### 1.3. Apply patch (RUN ON NODE 0 and 1)
```
# change variable value according to folder location
cassandra_dir=~/cs3114
t2c_dir=<t2c_absolute_dir>
script_dir=<this_file_parent_absolute_dir>

cd $t2c_dir

# compile t2c
./run_engine.sh compile

# apply patch
./run_engine.sh patch conf/samples/cs-3.11.5.properties cassandra

# build system
./run_engine.sh recover_tests conf/samples/cs-3.11.5.properties
```
### 2. Checkers generation (RUN ON NODE 0 and 1)
Run each command 1-by-1
```
cd $t2c_dir

# run retrofit
./run_engine.sh retrofit conf/samples/cs-3.11.5.properties 

# build checkers
./run_engine.sh build conf/samples/cs-3.11.5.properties

# use build output as validate input
cp -r $cassandra_dir/templates_out/ $cassandra_dir/templates_in/

# validate checkers
./run_engine.sh validate conf/samples/cs-3.11.5.properties

# copy valid checkers
rm -rf $cassandra_dir/templates_in/
cp -r $t2c_dir/inv_verify_output/verified_inv_dir $cassandra_dir
mv $t2c_dir/verified_inv_dir  $cassandra_dir/templates_in/
```
### 3. Bug Detection
#### 3.1. Modify cs config (ALL NODES, ON BOTH ~/cs2117 and ~/cs3114)
Modify `~/cassandra/conf/cassandra.yaml`

Replace `127.0.0.1` with the hostnames of nodes in your cluster.
```yaml
seed_provider:
    # Addresses of hosts that are deemed contact points. 
    # Cassandra nodes use this list of hosts to find each other and learn
    # the topology of the ring.  You must change this if you are running
    # multiple nodes!
    - class_name: org.apache.cassandra.locator.SimpleSeedProvider
      parameters:
          # seeds is actually a comma-delimited list of addresses.
          # Ex: "<ip1>,<ip2>,<ip3>"
          - seeds: "node1, node2, node3, node4, node5"
```

Replace `localhost` with the host name of your node.
```yaml
listen_address: hostname
```

Replace `initial_token` with these values
```yaml
# node 0
initial_token: 0

# node 1
initial_token: 56713727820156410577229101238628035242

# node 0
initial_token: 113427455640312821154458202477256070484
```

Replace `data_file_directories`
```yaml
data_file_directories:
    - /var/lib/cassandra/data
```

Replace `partitioner`
```yaml
org.apache.cassandra.dht.RandomPartitioner
```
#### 3.1. Run cassandra 2.11.7 (ON ALL NODES)
```
cd ~/cs2117
./bin/cassandra
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status
```
#### 3.2. Setup table (ONLY ON NODE 0)
```
./bin/cqlsh -f "<t2c_dir>/experiments/detection/cassandra/CASSANDRA-15072/commands/setup.cql"

./bin/nodetool -h ::FFFF:127.0.0.1 flush
```
#### 3.3. Force drain (ON ALL NODES)
```
./bin/nodetool -h ::FFFF:127.0.0.1 drain
```

#### 3.4. Stop and remove logs (ON ALL NODES)
```
echo "Cleaning up"
user=$(whoami)
pgrep -u "$user" -f cassandra | xargs kill -9

rm /var/lib/cassandra/data/commitlogs/*.log
```
#### 3.5. Restart cassandra 2.11.7 (ON ALL NODES)
```
cd ~/cs2117
./bin/cassandra
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status
```
#### 3.6. Upgrade node 1 (ONLY ON NODE 1)
```
./bin/nodetool -h ::FFFF:127.0.0.1 flush
./bin/nodetool -h ::FFFF:127.0.0.1 drain

echo "Cleaning up"
user=$(whoami)
pgrep -u "$user" -f cassandra | xargs kill -9

rm /var/lib/cassandra/data/commitlogs/*.log

cd ~/cs3114
./bin/cassandra
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status
```
#### 3.7. Upgrade node 2 (ONLY ON NODE 2)
```
./bin/nodetool -h ::FFFF:127.0.0.1 flush
./bin/nodetool -h ::FFFF:127.0.0.1 drain

echo "Cleaning up"
user=$(whoami)
pgrep -u "$user" -f cassandra | xargs kill -9

rm /var/lib/cassandra/data/commitlogs/*.log

cd ~/cs3114
./bin/cassandra
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status
```
#### 3.8. Flush cluster (ONLY ON NODE 0)
```
./bin/nodetool -h ::FFFF:127.0.0.1 flush
```
#### 3.9. Drain all nodes (ON ALL NODES)
```
./bin/nodetool -h ::FFFF:127.0.0.1 drain
```
#### 3.10. Stop all nodes (ON ALL NODES)
```
echo "Cleaning up"
user=$(whoami)
pgrep -u "$user" -f cassandra | xargs kill -9

rm /var/lib/cassandra/data/commitlogs/*.log
```
#### 3.11. Restart node 0 (ONLY ON NODE 0)
```
cd ~/cs2117
./bin/cassandra
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status
```
#### 3.12. Restart node 1 and 2 (ONLY ON NODE 1 AND 2)
```
cd ~/cs3114
./bin/cassandra
sleep 10
./bin/nodetool -h ::FFFF:127.0.0.1 status
```
#### 3.13. Run query (ON ALL NODES)
Check that they produce different result
```
./bin/cqlsh -f "<t2c_dir>/experiments/detection/cassandra/CASSANDRA-15072/commands/query.cql"
```

### 4. Cleanup
```
cd $script_dir
./kill.sh
```
