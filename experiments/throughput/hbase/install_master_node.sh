script_path=$(cd "$(dirname "$0")" && pwd)
hbase_path=/localtmp/hbase
hdfs_path=~/hdfs_for_hbase

echo "script_path: $script_path"
cd ~
# echo "#### installing hbase-2.4.0 in /localtmp/hbase"
# wget https://archive.apache.org/dist/hbase/2.4.0/hbase-2.4.0-src.tar.gz || exit
# tar -xzf hbase-2.4.0-src.tar.gz || exit
# rm hbase-2.4.0-src.tar.gz

cd "$script_path"
cp -f ./hbase-env.sh ./hbase-site.xml ./regionservers $hbase_path/conf/ || exit
cd $hbase_path
mvn package -DskipTests

echo "#### installing hadoop in ~/hdfs_for_hbase"
git clone https://github.com/apache/hadoop.git hdfs_for_hbase
cd hdfs_for_hbase && git checkout rel/release-3.2.2

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
