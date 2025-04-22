#usage:
#scripts/runtest/zk-3.4.11.sh [sys_root] [conf]
#example: (use absolute path!)
#scripts/runtest/zk-3.4.11.sh ~/zookeeper/ ~/T2C/conf/samples/zk-3.4.11.properties

T2CJARPATH="$PWD/target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar/"
ZKPATH=$1
CONFPATH=$2
#order matters here! otherwise there is potential conflicts between classes
CLASSPATH="${T2CJARPATH}:${ZKPATH}/build/test/classes/:${ZKPATH}/lib/*:${ZKPATH}/build/classes/:${ZKPATH}/build/lib/*:${ZKPATH}/build/test/lib/*"
#this script will switch under the repo of target system! since we might need to use target system's test data and tmp dir
(cd ${ZKPATH} && java -cp ${CLASSPATH} -Dconf=${CONFPATH} edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine)
