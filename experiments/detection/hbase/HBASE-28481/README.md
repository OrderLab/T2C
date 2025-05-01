# HBASE-28481

## Without T2C
```
./install_HBASE-28481.sh <hbase_absolute_path>
./trigger_HBASE-28481.sh <hbase_absolute_path>
```

## With T2C
### 1. Preparation
#### 1.1. Modify t2c config
In <t2c_dir>/conf/samples/hb-2.4.properties, modify `system_dir_path` to the hbase folder absolute path

#### 1.2. Apply patch
```
# change variable value according to folder location
hbase_dir=<hbase_absolute_dir>
t2c_dir=<t2c_absolute_dir>
script_dir=<this_file_parent_absolute_dir>

cd $hbase_dir
# checkout to buggy version of hbase
git checkout tags/rel/2.4.13

cd $t2c_dir

# compile t2c
./run_engine.sh compile

# apply patch
./run_engine.sh patch conf/samples/hb-2.4.properties hbase

# build system
./run_engine.sh recover_tests conf/samples/hb-2.4.properties
```
### 2. Checkers generation
Run each command 1-by-1
```
cd $t2c_dir

# run retrofit
./run_engine.sh retrofit conf/samples/hb-2.4.properties 

# build checkers
./run_engine.sh build conf/samples/hb-2.4.properties

# use build output as validate input
cp -r $hbase_dir/templates_out/ $hbase_dir/templates_in/

# validate checkers
./run_engine.sh validate conf/samples/hb-2.4.properties

# copy valid checkers
rm -rf $hbase_dir/templates_in/
cp -r $t2c_dir/inv_verify_output/verified_inv_dir $hbase_dir
mv $hbase_dir/verified_inv_dir  $hbase_dir/templates_in/
```
### 3. Bug Detection
#### 3.1. Run hbase
```
cd $script_dir
./trigger_HBASE-28481.sh <hbase_absolute_path>
```

#### 3.2. Check failed checkers
Check t2c.prod.log to see the failed checker list
