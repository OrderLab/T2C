#!/usr/bin/env bash
#
# The T2C Project
#
# Copyright (c) 2018, Johns Hopkins University - Order Lab.
#     All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Simply dump jimple files for Cassandra

my_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)
. ${my_dir}/common.sh

target_dir_str="CASSANDRA_DIR"
parse_args "$@"
csd_dir=$analysi_target_dir
if [ ! -d ${csd_dir} ]; then
  echo "Cassandra dir does not exist ${csd_dir}"
  exit 1
fi
csd_build_dir=${csd_dir}/build/classes/main
if [ ! -d ${csd_build_dir} ]; then
  echo "Could not find ${csd_build_dir}, have you built Cassandra?"
  exit 1
fi
if [ -d ${csd_build_dir}/edu/jhu/order/t2c ]; then
  echo "Cassandra already instrumented with T2C. Clean it first..."
  cd ${csd_dir}
  ant clean && ant jar
fi

# clean up last result
rm -rf ${out_dir}/*
mkdir -p ${out_dir}

# analyze all classes in Cassandra and specify the main class
run_t2c -a wjtp.mutationlogger -i ${csd_build_dir} -m org.apache.cassandra.service.CassandraDaemon \
  --prefix org.apache \
 -e -p jb use-original-names:true

cd ${out_dir}
jar -xvf ${lib_dir}/cloning-1.9.9-modified.jar > /dev/null
jar -xvf ${lib_dir}/objenesis-2.6.jar > /dev/null
cd ..
if [ $instrument -eq 1 ]; then
  echo "Instrumenting t2c driver back to Cassandra source in $csd_dir"
  mkdir -p ${csd_build_dir}/org ${csd_build_dir}/com
  cp -r ${out_dir}/org/apache ${csd_build_dir}/org
  cp -r ${out_dir}/org/objenesis ${csd_build_dir}/org
else
  echo "T2C parsed results are generated in $out_dir"
fi