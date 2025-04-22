#usage:
#scripts/installjar/zk-3.4.11.sh [sys_root]
#example: (use absolute path!)
#scripts/installjar/zk-3.4.11.sh ~/zookeeper/ 

T2CJARPATH="$PWD/target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar/"
ZKPATH=$1
(mkdir -p $1/build/lib && cp $PWD/target/*.jar $1/build/lib)
