script_path=$(cd "$(dirname "$0")" && pwd)
hbase_path=/localtmp/hbase
hdfs_path=/localtmp/hdfs

echo "script_path: $script_path"
cd ~
echo "#### installing hbase-2.4.0 in /localtmp/hbase"
# wget https://archive.apache.org/dist/hbase/2.4.0/hbase-2.4.0-src.tar.gz || exit
# tar -xzf hbase-2.4.0-src.tar.gz || exit
# rm hbase-2.4.0-src.tar.gz

cd "$script_path"
cp -f ./hbase-env.sh ./hbase-site.xml ./regionservers $hbase_path/conf/ || exit
cd $hbase_path
mvn package -DskipTests

echo "#### installing hadoop in ~/hadoop_for_hbase/hadoop-3.2.2-src"
# mkdir -p ~/hadoop_for_hbase && cd ~/hadoop_for_hbase
# wget https://archive.apache.org/dist/hadoop/common/hadoop-3.2.2/hadoop-3.2.2-src.tar.gz || exit
# tar -zxf hadoop-3.2.2-src.tar.gz || exit
# rm hadoop-3.2.2-src.tar.gz

cd "$script_path"
cp -f ./hdfs-site.xml $hdfs_path/hadoop-hdfs-project/hadoop-hdfs/src/main/conf
cp -f ./core-site.xml ./hadoop-env.sh $hdfs_path/hadoop-common-project/hadoop-common/src/main/conf
cd $hdfs_path
mvn package -Pdist -DskipTests -Dtar -Dmaven.javadoc.skip=true

cd "$script_path"
cd $hdfs_path/hadoop-dist/target/hadoop-3.2.2
echo "#### format namenode and start hadoop service"
export PDSH_RCMD_TYPE=ssh
./bin/hdfs namenode -format || exit
./sbin/start-dfs.sh || exit
sleep 2
jps
./bin/hdfs dfsadmin -report
