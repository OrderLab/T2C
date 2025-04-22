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

# Find the common.sh in lib; to be sourced by scripts inside the instrument 
# directory. Should NOT be directly executed.

function display_usage()
{
  cat <<EOF
Usage: 
  $0 [options] $target_dir_str

  -h: display this message

  -i, --instrument: instrument the watchdogs back to the $target_dir_str. If this
                    option is not specified, the generated watchdogs will reside
                    in sootOutput dir.

EOF
}

function parse_args()
{
  if [ "$1" == "-i" -o "$1" == "--instrument" ]; then
    instrument=1
    if [ $# -ne 2 ]; then
      >&2 display_usage 
      exit 1
    fi
    analysi_target_dir=$2
  elif [ "$1" == "-h" -o "$1" == "--help" ]; then
    display_usage
    exit 0
  else
    if [ $# -ne 1 ]; then
      >&2 display_usage
      exit 1
    fi
    analysi_target_dir=$1
  fi
}

this_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)
common_src=$(dirname "${this_dir}")/lib/common.sh
if [ ! -f ${common_src} ]; then
  echo "Could not find ${common_src}"
  exit 1
fi
. ${common_src}
if [ -z "${out_dir}" -o ${out_dir} == "." -o ${out_dir} == "/" ]; then
  echo "Output dir set to '${out_dir}', will shoot ourselves in the foot..."
  exit 1
fi
instrument=0
analysi_target_dir=
target_dir_str="TARGET_DIR"
