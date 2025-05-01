# HDFS-14699

## Without T2C
```
./install_HDFS-14699.sh <hdfs_absolute_path>
./trigger_HDFS-14699.sh <hdfs_absolute_path>
```

## With T2C
### 1. Preparation
#### 1.1. Modify t2c config
In <t2c_dir>/conf/samples/hdfs-3.1.3.properties, modify `system_dir_path` to the hdfs folder absolute path

#### 1.2. Apply patch
```
# change variable value according to folder location
hdfs_dir=<hdfs_absolute_dir>
t2c_dir=<t2c_absolute_dir>
script_dir=<this_file_parent_absolute_dir>
version=3.1.3

cd $hdfs_dir
# checkout to buggy version of hdfs
git checkout tags/rel/release-3.1.3

cd $t2c_dir

# compile t2c
./run_engine.sh compile

# apply patch
./run_engine.sh patch conf/samples/hdfs-3.1.3.properties hdfs

# build system
./run_engine.sh recover_tests conf/samples/hdfs-3.1.3.properties

# hdfs needs to apply patch again after recover
./run_engine.sh patch conf/samples/hdfs-3.1.3.properties hdfs

# copy config files
cp $script_dir/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
```
### 2. Checkers generation
Run each command 1-by-1
```
cd $t2c_dir

# run retrofit
./run_engine.sh retrofit conf/samples/hdfs-3.1.3.properties 

# build checkers
./run_engine.sh build conf/samples/hdfs-3.1.3.properties

# use build output as validate input
cp -r $hdfs_dir/templates_out/ $hdfs_dir/templates_in/
cp -r $hdfs_dir/templates_in/ ~/templates_in/

# validate checkers
./run_engine.sh validate conf/samples/hdfs-3.1.3.properties

# copy valid checkers to  ~ for hdfs
rm -rf ~/templates_in
cp -r $t2c_dir/inv_verify_output/verified_inv_dir ~
mv ~/verified_inv_dir ~/templates_in
```
### 3. Bug Detection
#### 3.1. Apply additional patch to reproduce failure
```
# apply failure-specific patch
cd $hadoop_dir
git apply $script_dir/hook_HDFS-14699.patch

cd $t2c_dir

# build system
./run_engine.sh recover_tests conf/samples/hdfs-3.1.3.properties

# hdfs needs to apply patch again after recover
./run_engine.sh patch conf/samples/hdfs-3.1.3.properties hdfs

# run retrofit
./run_engine.sh retrofit conf/samples/hdfs-3.1.3.properties 

```
#### 3.2. Run hdfs
```
cd $script_dir
./trigger_HDFS-14699.sh <hdfs_absolute_path>
```

#### 3.3. Check failed checkers
Check t2c.prod.log to see the failed checker list

### 4. Cleanup
```
cd $script_dir
./kill.sh <hdfs_absolute_path>
```
