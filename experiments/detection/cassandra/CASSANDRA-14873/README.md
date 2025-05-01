# CS-14873

## Without T2C
```
./all_CASSANDRA-14873.sh <cassandra_absolute_path>
```

## With T2C
### 1. Preparation
#### 1.1. Modify t2c config
In <t2c_dir>/conf/samples/cs-3.11.5.properties, modify `system_dir_path` to the cassandra folder absolute path.

#### 1.2. Apply patch
```
# change variable value according to folder location
cassandra_dir=<cassandra_absolute_dir>
t2c_dir=<t2c_absolute_dir>
script_dir=<this_file_parent_absolute_dir>

cd $cassandra_dir
# checkout to buggy version of cassandra
git checkout tags/cassandra-3.11.3

cd $t2c_dir

# compile t2c
./run_engine.sh compile

# apply patch
./run_engine.sh patch conf/samples/cs-3.11.5.properties cassandra

# build system
./run_engine.sh recover_tests conf/samples/cs-3.11.5.properties
```
### 2. Checkers generation
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
mv $zookeeper_dir/verified_inv_dir  $cassandra_dir/templates_in/
```
### 3. Bug Detection
#### 3.1. Run cassandra
```
cd $script_dir
./trigger_CASSANDRA-14873.sh <cassandra_absolute_path>
```

#### 3.2. Check failed checkers
Check t2c.prod.log to see the failed checker list

### 4. Cleanup
```
cd $script_dir
./kill.sh
```
