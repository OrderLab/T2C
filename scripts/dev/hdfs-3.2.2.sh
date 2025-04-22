#this script would automatically do some processing and start testing on an instance

#usage:
#scripts/dev/hdfs-3.2.2.sh [sys_root]
#example: (use absolute path!)
#scripts/dev/hdfs-3.2.2.sh ~/zookeeper/

SYSPATH=$1
COMPILE_COMMANDS="mvn -U clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -pl hadoop-common-project/hadoop-common,\
hadoop-common-project/hadoop-nfs,hadoop-common-project/hadoop-kms,\
hadoop-hdfs-project/hadoop-hdfs,\
hadoop-hdfs-project/hadoop-hdfs-client,hadoop-hdfs-project/hadoop-hdfs-httpfs,hadoop-hdfs-project/hadoop-hdfs-native-client,\
hadoop-hdfs-project/hadoop-hdfs-nfs,hadoop-hdfs-project/hadoop-hdfs-rbf -am"

echo "rebuild T2C"
mvn clean package -DskipTests
echo "clean HDFS build"
(cd ${SYSPATH} && rm -rf build)
echo "install jar to HDFS"
scripts/installjar/hdfs-3.2.2.sh ${SYSPATH}
echo "compile HDFS build and test"
(cd ${SYSPATH} && ${COMPILE_COMMANDS}) # this should be system specific compilation command
echo "run assertpack"
scripts/analysis/assertpack_hdfs-3.2.2.sh ${SYSPATH} 
echo "generate template"
scripts/runtest/hdfs-3.2.2.sh ${SYSPATH} $(PWD)/conf/samples/hdfs-3.2.2.properties
echo "copy templates"
(cd ${SYSPATH} && cp -r templates_out/ templates_in/)
echo "done."



