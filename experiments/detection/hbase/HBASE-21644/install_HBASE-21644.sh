#! /bin/bash
# $1 is hbase dir

script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )
cd "$1" || exit 1

git checkout 2776bc0 #For patched version, checkout c5f4e84
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar