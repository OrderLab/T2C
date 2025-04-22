#! /bin/bash
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1

#run this scripts on all nodes
git checkout tags/rel/release-3.2.1
git apply $1/experiments/reproduce/HDFS-14699/hook_HDFS-14699.patch
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
version='3.2.1'

cp $script_dir/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/workers hadoop-dist/target/hadoop-${version}/etc/hadoop/

sed -i 's/export JAVA_HOME=${JAVA_HOME}/JAVA_HOME=\/usr\/lib\/jvm\/java-1.8.0-openjdk-amd64\//g' hadoop-dist/target/hadoop-3.2.1/etc/hadoop/hadoop-env.sh
sed -i 's/# export KMS_LOG=${KMS_HOME}\/logs/export KMS_LOG=${KMS_HOME}\/logs/g' hadoop-dist/target/hadoop-3.2.1/etc/hadoop/kms-env.sh
sed -i 's/# export KMS_HTTP_PORT=16000/export KMS_HTTP_PORT=16000/g' hadoop-dist/target/hadoop-3.2.1/etc/hadoop/kms-env.sh
sed -i 's/# export KMS_ADMIN_PORT=`expr ${KMS_HTTP_PORT} + 1`/export KMS_ADMIN_PORT=`expr ${KMS_HTTP_PORT} + 1`/g' hadoop-dist/target/hadoop-3.2.1/etc/hadoop/kms-env.sh
