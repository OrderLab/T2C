# ZK-4325

## Without T2C
```
./install_ZK-4325.sh <zk_absolute_path>
./trigger_ZK-4325.sh <zk_absolute_path>
./kill.sh <zk_absolute_path>
```

## With T2C
### 1. Preparation
#### 1.1. Modify t2c config
In <t2c_dir>/conf/samples/zk-3.6.2.properties, modify `system_dir_path` to the zookeeper folder absolute path, and modify `patch_path` to `${t2c_dir}/conf/samples/patches/install_zk-4325.patch`

#### 1.2. Apply patch
```
# change variable value according to folder location
zookeeper_dir=<zookeeper_absolute_dir>
t2c_dir=<t2c_absolute_dir>
script_dir=<this_file_parent_absolute_dir>

cd $zookeeper_dir
# checkout to buggy version of zookeeper
git checkout tags/release-3.6.2
# generate zookeeper config
cp $script_dir/zoo.cfg ./conf
echo dataDir=$zookeeper_dir >> ./conf/zoo.cfg

cd $t2c_dir

# compile t2c
./run_engine.sh compile

# apply patch
./run_engine.sh patch conf/samples/zk-3.6.2.properties zookeeper

# build system
./run_engine.sh recover_tests conf/samples/zk-3.6.2.properties
```
### 2. Checkers generation
Run each command 1-by-1
```
cd $t2c_dir

# run retrofit
./run_engine.sh retrofit conf/samples/zk-3.6.2.properties 

# build checkers
./run_engine.sh build conf/samples/zk-3.6.2.properties

# use build output as validate input
cp -r $zookeeper_dir/templates_out/ $zookeeper_dir/templates_in/

# validate checkers
./run_engine.sh validate conf/samples/zk-3.6.2.properties

# copy valid checkers
rm -rf $zookeeper_dir/templates_in/
cp -r $t2c_dir/inv_verify_output/verified_inv_dir $zookeeper_dir
mv $zookeeper_dir/verified_inv_dir  $zookeeper_dir/templates_in/
```
### 3. Bug Detection
#### 3.1. Run Zookeeper
```
cd $script_dir
./trigger_ZK-4325.sh <zk_absolute_path>
```

#### 3.2. Check failed checkers
Check t2c.prod.log to see the failed checker list

### 4. Cleanup
```
cd $script_dir
./kill.sh <zk_absolute_path>
```
