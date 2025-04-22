#! /bin/bash

# run this scripts on two namenodes. No need to run on NFS server.
# version: 2.9.2

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1
version='2.9.2'
git checkout tags/rel/release-$version
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
cp $script_dir/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/kms-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/kms.passwd hadoop-dist/target/hadoop-${version}/share/hadoop/kms/tomcat/webapps/kms/WEB-INF/classes
# configure in hadoop-env.sh and kms-env.sh
sed -i 's/export JAVA_HOME=${JAVA_HOME}/JAVA_HOME=\/usr\/lib\/jvm\/java-1.8.0-openjdk-amd64\//g' hadoop-dist/target/hadoop-2.9.2/etc/hadoop/hadoop-env.sh
sed -i 's/# export KMS_LOG=${KMS_HOME}\/logs/export KMS_LOG=${KMS_HOME}\/logs/g' hadoop-dist/target/hadoop-2.9.2/etc/hadoop/kms-env.sh
sed -i 's/# export KMS_HTTP_PORT=16000/export KMS_HTTP_PORT=16000/g' hadoop-dist/target/hadoop-2.9.2/etc/hadoop/kms-env.sh
sed -i 's/# export KMS_ADMIN_PORT=`expr ${KMS_HTTP_PORT} + 1`/export KMS_ADMIN_PORT=`expr ${KMS_HTTP_PORT} + 1`/g' hadoop-dist/target/hadoop-2.9.2/etc/hadoop/kms-env.sh