script_path=$(cd "$(dirname "$0")" && pwd)
hbase_path=/localtmp/hbase
hdfs_path=~/hdfs_for_hbase

echo "script_path: $script_path"
cd ~
# echo "#### installing hbase-2.4.0 in ~/hbase-2.4.0"
# wget https://archive.apache.org/dist/hbase/2.4.0/hbase-2.4.0-src.tar.gz || exit
# tar -xzf hbase-2.4.0-src.tar.gz || exit
# rm hbase-2.4.0-src.tar.gz
cd "$script_path"
cp -f ./hbase-env.sh ./hbase-site.xml ./regionservers $hbase_path/conf/ || exit
cd $hbase_path
mvn package -DskipTests