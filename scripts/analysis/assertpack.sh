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

# Transform test cases to generate assertion func
# hints: to only execute specific classes plz use internalTransformSpecificClasses in the codes
# Usage:
# scripts/analysis/assertpack.sh [class_dir] 
# for example, to run test case
# scripts/analysis/assertpack.sh target/test-classes/ 

my_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)
#we need to include assert class so we can insert assert append hook
target_dir=target/classes/
. ${my_dir}/common.sh

# clean up last result
rm -rf ${out_dir}
mkdir -p ${out_dir}

# analyze all classes in zookeeper and specify the main class
#run_t2c -a wjtp.testassertionpacker -i $1 ${target_dir} -n -o ${out_dir} -w -m $5 -p jb model-lambdametafactory:false \
run_t2c -a wjtp.testassertionpacker -i $1 ${target_dir} -n -o ${out_dir} -w -m $5 -p jb use-original-names:true,model-lambdametafactory:false \
 --config test_name:$2 test_method:$3 conf:$4

