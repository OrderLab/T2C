#!/usr/bin/env bash
#
# @author Ryan Huang <huang@cs.jhu.edu>
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

# The main T2C script

bin_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)
env_source="${bin_dir}/t2c-env.sh"

if [ ! -f ${env_source} ]; then
  echo "Could not find the env source ${env_source}"
  exit 1
fi
# source the envs
. ${env_source}

(set -x; 
"${JAVA}" -cp ${T2C_CLASSPATH} ${T2C_JAVA_OPTS} ${T2C_MAIN} -x ${SOOT_CLASSPATPH} "$@"
)
