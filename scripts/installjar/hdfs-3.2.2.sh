#usage:
#scripts/installjar/hdfs-3.2.2.sh [sys_root]
#example: (use absolute path!)
#scripts/installjar/hdfs-3.2.2.sh ~/zookeeper/ 

T2CJARPATH="$PWD/target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar/"
(mkdir -p $1/build/lib && cp $PWD/target/*.jar $1/build/lib)
