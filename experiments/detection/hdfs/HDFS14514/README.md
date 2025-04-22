# HDFS-14514

## Without T2C
```
./install_HDFS-14514.sh <hdfs_absolute_path>
./trigger_HDFS-14514.sh <hdfs_absolute_path>
```

## With T2C
### 1. Preparation
#### 1.1. Modify t2c config
In <t2c_dir>/conf/samples/hdfs-2.9.2.properties, modify `system_dir_path` to the hdfs folder absolute path

#### 1.2. Apply patch
```
# change variable value according to folder location
hdfs_dir=<hdfs_absolute_dir>
t2c_dir=<t2c_absolute_dir>
script_dir=<this_file_parent_absolute_dir>
version=2.9.2

cd $hdfs_dir
# checkout to buggy version of hdfs
git checkout tags/rel/release-2.9.2

cd $t2c_dir

# compile t2c
./run_engine.sh compile

# apply patch
./run_engine.sh patch conf/samples/hdfs-2.9.2.properties hdfs

# build system
./run_engine.sh recover_tests conf/samples/hdfs-2.9.2.properties

# hdfs needs to apply patch again after recover
./run_engine.sh patch conf/samples/hdfs-2.9.2.properties hdfs

# copy config files
cp $script_dir/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
```
### 2. Checkers generation
Run each command 1-by-1
```
cd $t2c_dir

# run retrofit
./run_engine.sh retrofit conf/samples/hdfs-2.9.2.properties 

# build checkers
./run_engine.sh build conf/samples/hdfs-2.9.2.properties

# use build output as validate input
cp -r $hdfs_dir/templates_out/ $hdfs_dir/templates_in/

# validate checkers
./run_engine.sh validate conf/samples/hdfs-2.9.2.properties

# copy valid checkers to  ~ for hdfs
cp -r $t2c_dir/inv_verify_output/templates_in ~
```
### 3. Bug Detection
#### 3.1. Run hdfs
```
cd $script_dir
./trigger_HDFS-14514.sh <hdfs_absolute_path>
```

#### 3.2. Check failed checkers
Check t2c.prod.log to see the failed checker list

### 4. Cleanup
```
cd $script_dir
./kill.sh <hdfs_absolute_path>
```
