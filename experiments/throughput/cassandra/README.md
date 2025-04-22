This is a brief instruction for cassandra throughput evaluation in a 5-node cluster.

### Install

Fist run `install.sh` in **every node**.

```shell
./install.sh
```

Then modify `~/cassandra/conf/cassandra.yaml` in **every node**:

In line 425, replace `127.0.0.1` with the hostnames of nodes in your cluster.

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

In line 612, replace `localhost` with the host name of your node.

```yaml
listen_address: hostname
```

### Start Experiments

Start cassandra in **every node**:

```shell
cd ~/cassandra && ./bin/cassandra
```

Run `throughput.sh` **only in one node**, for example, node1.

```shell
./throughput.sh
```

> Please note that there are some prompts starting with `####` in terminal after running `throughput.sh`, which tell you where to find the results.

Then check the results in terminal and `~/YCSB`.

### Clean up and Repeat

Run `./cleanup.sh` in **every node**.

```shell
./cleanup.sh
```

Then repeat `Start Experiments` section.