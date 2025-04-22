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

# Common variables to use

function run_t2c() {
  echo ${t2c} -o ${out_dir} "$@"
  ${t2c} -o ${out_dir} "$@"
  if [ $? -ne 0 ]; then
    exit $?
  fi
}

function check_dir() {
  if [ ! -d $1 ]; then
    echo "Could not find directory $1"
    exit 1
  fi
}

scripts_common_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)
root_dir=$(dirname $(dirname "${scripts_common_dir}"))  # root is ../..
bin_dir="${root_dir}/bin"
out_dir=${root_dir}/sootOutput
test_dir=${root_dir}/test
lib_dir=${root_dir}/lib
t2c=${bin_dir}/t2c.sh

check_dir ${bin_dir}
check_dir ${lib_dir}

if [ ! -x ${t2c} ]; then
  echo "Could not find ${t2c}"
  exit 1
fi
