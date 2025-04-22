#run this scripts on namenode 

HP_HOME () {
  version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < $1/pom.xml)
  cd $1/hadoop-dist/target/hadoop-${version}
}
HP_HOME $1
bin/hdfs namenode org.apache.hadoop.hdfs.server.namenode.NameNode -format orderlab

sleep 20
rm -rf /tmp/hdfs/vol1/dn /tmp/hdfs/vol2/dn /tmp/hdfs/vol3/dn
sbin/start-dfs.sh

echo "sleep 10 sec to wait for instances start up"
bin/hadoop org.apache.hadoop.hdfs.NNBenchWithoutMR -startTime 180 -operation createWrite -baseDir /benchmarks -numFiles 100 -blocksPerFile 160 -bytesPerBlock 1048576
