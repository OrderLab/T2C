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

# Simply dump jimple files for zookeeper

my_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)
. ${my_dir}/common.sh

target_dir_str="ZOOKEEPER_DIR"
parse_args "$@"
zk_dir=$analysi_target_dir

if [ ! -d ${zk_dir} ]; then
  echo "ZooKeeper dir does not exist ${zk_dir}"
  exit 1
fi
zk_build_dir=${zk_dir}/build/classes
zk_quorum_dir=${zk_build_dir}/org/apache/zookeeper/server/quorum
if [ ! -d ${zk_quorum_dir} ]; then
  echo "Could not find ${zk_quorum_dir}, have you built ZooKeeper?"
  exit 1
fi
if [ -d ${zk_build_dir}/edu/jhu/order/t2c ]; then
  echo "ZooKeeper already instrumented with T2C. Clean it first..."
  cd ${zk_dir}
  ant clean && ant jar
fi

# clean up last result
rm -rf ${out_dir}/*
mkdir -p ${out_dir}

# analyze all classes in zookeeper and specify the main class
run_t2c -a wjtp.simplejimpler -i ${zk_build_dir} -m org.apache.zookeeper.server.quorum.QuorumPeerMain -p jb use-original-names:true 
