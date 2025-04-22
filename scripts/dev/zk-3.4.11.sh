#this script would automatically do some processing and start testing on an instance

#usage:
#scripts/dev/zk-3.4.11.sh [sys_root]
#example: (use absolute path!)
#scripts/dev/zk-3.4.11.sh ~/zookeeper/

ZKPATH=$1
echo "rebuild T2C"
mvn clean package -DskipTests
echo "clean zookeeper build"
(cd ${ZKPATH} && rm -rf build)
echo "install jar to zookeeper"
scripts/installjar/zk-3.4.11.sh ${ZKPATH}
echo "compile zookeeper build and test"
(cd ${ZKPATH} && ant compile-test)
echo "run assertpack"
scripts/analysis/assertpack_zk-3.4.11.sh ${ZKPATH} 
echo "generate template"
scripts/runtest/zk-3.4.11.sh ${ZKPATH} $(pwd)/conf/samples/zk-3.4.11.properties
echo "copy templates"
(cd ${ZKPATH} && cp -r templates_out/ templates_in/)
#echo "start zk instance"
#(cd ${ZKPATH} && bin/zkServer.sh stop && bin/zkServer.sh start && bin/zkServer.sh status)
echo "done."



