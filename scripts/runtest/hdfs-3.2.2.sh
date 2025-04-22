#usage:
#scripts/runtest/hdfs-3.2.2.sh [sys_root] [conf]
#example: (use absolute path!)
#scripts/runtest/hdfs-3.2.2.sh ~/hadoop/ ~/T2C/conf/samples/hdfs-3.2.2.properties

T2CJARPATH="$PWD/target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar/"
SYSPATH=$1
CONFPATH=$2
system_dir_path=$1
system_version_suffix="3.2.1"
java_class_path="\
${system_dir_path}/hadoop-common-project/hadoop-common/target/hadoop-common-${system_version_suffix}/share/hadoop/common/*:\
${system_dir_path}/hadoop-common-project/hadoop-common/target/hadoop-common-${system_version_suffix}/share/hadoop/common/lib/*:\
${system_dir_path}/hadoop-common-project/hadoop-kms/target/hadoop-kms-${system_version_suffix}/share/hadoop/common/*:\
${system_dir_path}/hadoop-common-project/hadoop-kms/target/hadoop-kms-${system_version_suffix}/share/hadoop/common/lib/*:\
${system_dir_path}/hadoop-common-project/hadoop-nfs/target/hadoop-nfs-${system_version_suffix}/share/hadoop/common/*:\
${system_dir_path}/hadoop-common-project/hadoop-nfs/target/hadoop-nfs-${system_version_suffix}/share/hadoop/common/lib/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs/target/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-client/target/hadoop-hdfs-client-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-client/target/hadoop-hdfs-client-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-httpfs/target/hadoop-hdfs-httpfs-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-httpfs/target/hadoop-hdfs-httpfs-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-native-client/target/hadoop-hdfs-native-client-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-native-client/target/hadoop-hdfs-native-client-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-nfs/target/hadoop-hdfs-nfs-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-nfs/target/hadoop-hdfs-nfs-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-rbf/target/hadoop-hdfs-rbf-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-hdfs-project/hadoop-hdfs-rbf/target/hadoop-hdfs-rbf-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-dist/target/hadoop-${system_version_suffix}/share/hadoop/hdfs/*:\
${system_dir_path}/hadoop-dist/target/hadoop-${system_version_suffix}/share/hadoop/hdfs/lib/*:\
${system_dir_path}/hadoop-dist/target/hadoop-${system_version_suffix}/share/hadoop/common/*:\
${system_dir_path}/hadoop-dist/target/hadoop-${system_version_suffix}/share/hadoop/common/lib/*:\
${system_dir_path}/hadoop-dist/target/hadoop-${system_version_suffix}/share/hadoop/mapreduce/*:\
${system_dir_path}/hadoop-dist/target/hadoop-${system_version_suffix}/share/hadoop/mapreduce/lib/*"
#order matters here! otherwise there is potential conflicts between classes
CLASSPATH="${T2CJARPATH}:${SYSPATH}/hadoop-hdfs-project/hadoop-hdfs/target/test-classes/:${java_class_path}"
#this script will switch under the repo of target system! since we might need to use target system's test data and tmp dir
(cd ${SYSPATH} && java -cp ${CLASSPATH} -Dconf=${CONFPATH} edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine)
