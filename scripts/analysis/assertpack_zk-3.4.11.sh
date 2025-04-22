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

# Run 2 steps:
# 1) Transform [zookeeper] test cases to generate assertion func
# 2) Pack back to test jar
# Usage:
# scripts/analysis/assertpack_zk-3.4.11.sh [zk_root_dir] 
# for example, to run test case
# scripts/analysis/assertpack_zk-3.4.11.sh ~/zookeeper/build/test/classes/ 

#0) check if already modified before
marker=$1"/build/test/classes/assertpacker.mark"
if test -f "$marker"; then
    echo "$marker exists. Already transformed before, exit"
    exit
fi

#1) run assertpack on [zookeeper]
scripts/analysis/assertpack.sh $1"/build/test/classes/"

#2) pack test classes (assert classes && modified test classes) back, two locations are needed
#   a) test phase
#       zookeeper/build/test/classes
#   b) runtime phase
#       for 3.4.11, we would pack test classes to zookeeper/build/lib
cp -r sootOutput/org $1"/build/test/classes/"
jar cvf sootOutput/t2c-runtime.jar sootOutput/org
cp sootOutput/t2c-runtime.jar $1"/build/lib/"

#leave marker
touch $marker
