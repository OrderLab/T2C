#! /bin/bash

PATH_T2C=""
PATH_SYSTEM_BASE="~"
PROPERTIES=""

# ZK-1754
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/zookeeper.git zk1754
PATH_SYSTEM=$PATH_SYSTEM_BASE/zk1754
PROPERTIES="zk-3.4.11.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout e3c1c87739b9fede7a0fcad0aaad0ca65b5101a9

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_zk-1754.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES zookeeper
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# ZK-2355
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/zookeeper.git zk2355
PATH_SYSTEM=$PATH_SYSTEM_BASE/zk2355
PROPERTIES="zk-3.4.11.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout 69710181

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_zk-2355.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES zookeeper
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# ZK-4362
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/zookeeper.git zk4362
PATH_SYSTEM=$PATH_SYSTEM_BASE/zk4362
PROPERTIES="zk-3.6.2.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/release-3.6.3

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_zk-3.6.3-4362.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES zookeeper
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# CS-15072
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/cassandra.git cs15072
PATH_SYSTEM=$PATH_SYSTEM_BASE/cs15072
PROPERTIES="cs-3.11.5.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/cassandra-3.11.4

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_cs-3.11.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES cassandra
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# CS-14873
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/cassandra.git cs14873
PATH_SYSTEM=$PATH_SYSTEM_BASE/cs14873
PROPERTIES="cs-3.11.5.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/cassandra-3.11.3

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_cs-3.11.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES cassandra
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# CS-14092
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/cassandra.git cs14092
PATH_SYSTEM=$PATH_SYSTEM_BASE/cs14092
PROPERTIES="cs-3.11.5.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/cassandra-3.0.15

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_cs-3.0.15.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES cassandra
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# CS-14803
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/cassandra.git cs14803
PATH_SYSTEM=$PATH_SYSTEM_BASE/cs14803
PROPERTIES="cs-3.11.5.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/cassandra-3.11.3

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES
sed -i "s|patch_path=.*|patch_path=${t2c_dir}/conf/samples/patches/install_cs-3.11.patch|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES cassandra
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HDFS-14514
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hadoop.git hdfs14514
PATH_SYSTEM=$PATH_SYSTEM_BASE/hdfs14514
PROPERTIES="hdfs-2.9.2.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/release-2.9.2

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hdfs
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HDFS-14699
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hadoop.git hdfs14699
PATH_SYSTEM=$PATH_SYSTEM_BASE/hdfs14699
PROPERTIES="hdfs-3.1.3.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/release-3.1.3

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hdfs
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HDFS-16942
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hadoop.git hdfs16942
PATH_SYSTEM=$PATH_SYSTEM_BASE/hdfs16942
PROPERTIES="hdfs-3.1.3.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/release-3.1.3

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hdfs
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HBase-21041
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hbase.git hbase21041
PATH_SYSTEM=$PATH_SYSTEM_BASE/hbase21041
PROPERTIES="hb-2.4.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/2.1.0

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hbase
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HBase-28481
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hbase.git hbase28481
PATH_SYSTEM=$PATH_SYSTEM_BASE/hbase28481
PROPERTIES="hb-2.4.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/2.4.13

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hbase
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HBase-25827
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hbase.git hbase25827
PATH_SYSTEM=$PATH_SYSTEM_BASE/hbase25827
PROPERTIES="hb-2.4.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/2.1.0

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hbase
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/

# HBase-21621
cd $PATH_SYSTEM_BASE
git clone https://github.com/apache/hbase.git hbase21621
PATH_SYSTEM=$PATH_SYSTEM_BASE/hbase21621
PROPERTIES="hb-2.4.properties"

cd $PATH_SYSTEM
git reset --hard
git clean -fdx
git checkout tags/rel/2.1.1

cd $PATH_T2C
sed -i "s|system_dir_path=.*|system_dir_path=$PATH_SYSTEM|g" $PATH_T2C/conf/samples/$PROPERTIES

./run_engine.sh compile
./run_engine.sh patch conf/samples/$PROPERTIES hbase
./run_engine.sh recover_tests conf/samples/$PROPERTIES
./run_engine.sh retrofit conf/samples/$PROPERTIES 
./run_engine.sh build conf/samples/$PROPERTIES
cp -r $PATH_SYSTEM/templates_out/ $PATH_SYSTEM/templates_in/
./run_engine.sh validate conf/samples/$PROPERTIES

rm -rf $PATH_SYSTEM/templates_in/
cp -r $PATH_T2C/inv_verify_output/verified_inv_dir $PATH_SYSTEM
mv $PATH_SYSTEM/verified_inv_dir  $PATH_SYSTEM/templates_in/
