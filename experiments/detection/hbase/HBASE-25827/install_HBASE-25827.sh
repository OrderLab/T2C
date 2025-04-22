#! /bin/bash
# $1 is hbase dir

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )
cd "$1" || exit 1

git checkout tags/rel/2.2.6
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar