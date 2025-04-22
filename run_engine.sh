#!/bin/bash
#
# The T2C Project
#
# Copyright (c) 2022, Johns Hopkins University - Order Lab.
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

#This is the main engine script for doing common user commands

#constants
single_command_timeout_threshold=1000h

t2c_dir=$(cd "$(dirname "${BASH_SOURCE-$0}")"; pwd)

echo "Setting tool root dir: "${t2c_dir}

if [[ -z "$JAVA_HOME" ]]; then
  export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")
  echo "Warning: JAVA_HOME env is not set, inferring it to be $JAVA_HOME"
fi

# check if we are running this scripts under T2C dir, if not, abort
# we check by scanning if we can find this run_engine script
# meanwhile, note that when running test cases we always use target system dir as working dir
if [ -f "${t2c_dir}/run_engine.sh" ]; then
    echo "The tool is running under T2C dir"
else
    echo "[ERROR] cannot detect run_engine.sh under root dir, please run this script under T2C root dir"
    echo "Abort."
    exit
fi

t2c_lib="${t2c_dir}/target/t2c-1.0-SNAPSHOT-jar-with-dependencies.jar"
log4j_conf="${t2c_dir}/conf/log4j.properties"
#this should be consistent with dir in codes
inv_out="${t2c_dir}/inv_infer_output"
trace_out="${t2c_dir}/trace_output"
check_out="${t2c_dir}/inv_checktrace_output"

banner (){
  echo -e ""
  echo " ▄▄▄▄▄▄▄▄▄▄▄  ▄▄▄▄▄▄▄▄▄▄▄  ▄▄▄▄▄▄▄▄▄▄▄          ▄▄▄▄         ▄▄▄▄▄▄▄▄▄  "
  echo "▐░░░░░░░░░░░▌▐░░░░░░░░░░░▌▐░░░░░░░░░░░▌       ▄█░░░░▌       ▐░░░░░░░░░▌ "
  echo " ▀▀▀▀█░█▀▀▀▀  ▀▀▀▀▀▀▀▀▀█░▌▐░█▀▀▀▀▀▀▀▀▀       ▐░░▌▐░░▌      ▐░█░█▀▀▀▀▀█░▌"
  echo "     ▐░▌               ▐░▌▐░▌                 ▀▀ ▐░░▌      ▐░▌▐░▌    ▐░▌"
  echo "     ▐░▌               ▐░▌▐░▌                    ▐░░▌      ▐░▌ ▐░▌   ▐░▌"
  echo "     ▐░▌      ▄▄▄▄▄▄▄▄▄█░▌▐░▌                    ▐░░▌      ▐░▌  ▐░▌  ▐░▌"
  echo "     ▐░▌     ▐░░░░░░░░░░░▌▐░▌                    ▐░░▌      ▐░▌   ▐░▌ ▐░▌"
  echo "     ▐░▌     ▐░█▀▀▀▀▀▀▀▀▀ ▐░▌                    ▐░░▌      ▐░▌    ▐░▌▐░▌"
  echo "     ▐░▌     ▐░█▄▄▄▄▄▄▄▄▄ ▐░█▄▄▄▄▄▄▄▄▄       ▄▄▄▄█░░█▄▄▄  ▄▐░█▄▄▄▄▄█░█░▌"
  echo "     ▐░▌     ▐░░░░░░░░░░░▌▐░░░░░░░░░░░▌     ▐░░░░░░░░░░░▌▐░▌▐░░░░░░░░░▌ "
  echo "      ▀       ▀▀▀▀▀▀▀▀▀▀▀  ▀▀▀▀▀▀▀▀▀▀▀       ▀▀▀▀▀▀▀▀▀▀▀  ▀  ▀▀▀▀▀▀▀▀▀  "
  echo -e ""
}

#generic functions
usage (){
  echo -e "T2C Engine Navigator: ./run_engine.sh [command] [args...]"
  echo -e ""
  echo -e "\t command:"
  echo -e ""
  echo -e "\t help              \t\t\t get help information"
  echo -e "\t compile           \t\t\t compile all T2C libraries"
  echo -e "\t patch    \t\t\t patch target system"
  echo -e "\t\t conf_file_path zookeeper|hdfs "
  echo -e "\t compile_target    \t\t\t compile target system"
  echo -e "\t\t conf_file_path "
  echo -e "\t retrofit          \t\t\t retrofit selected test case classes and encapsulate checking functions"
  echo -e "\t\t conf_file_path "
  echo -e "\t build             \t\t\t execute tests and build checker templates"
  echo -e "\t\t conf_file_path "
  echo -e "\t parallel_build    \t\t\t run build on multiple machines in parallel"
  echo -e "\t\t conf_file_path worker_file_path"
  echo -e "\t validate          \t\t\t validate mutated checker templates"
  echo -e "\t\t conf_file_path "
  echo -e "\t parallel_validate \t\t\t run validate on multiple machines in parallel"
  echo -e "\t\t conf_file_path worker_file_path"
  echo -e "\t install           \t\t\t install T2C runtime to target system, so 1) the T2C related annotations and functions \
   can be compiled in compilation phase 2) in production the T2C runtime can run together with target system"
  echo -e "\t\t conf_file_path "
  echo -e "\t recover_tests     \t\t\t clean failed retrofit attempts and recover tests"
  echo -e "\t\t conf_file_path "
  echo -e "\t test_runtime      \t\t\t start a runtime instance and test"
  echo -e "\t\t conf_file_path "
  echo -e ""
  echo -e "\t utility command:"
  echo -e ""
  echo -e "\t kill              \t\t\t kill on-going T2C tasks"
  echo -e "\t\t conf_file_path "
  echo -e "\t metrics           \t\t\t analyze the metrics of dumped templates"
  echo -e "\t\t conf_file_path template_dir_path"
  echo -e "\t sync_data         \t\t\t metrics of dumped templates"
  echo -e "\t\t conf_file_path worker_file_path path_to_sync_folder(e.g. /home/xx/hbase/templates_out)"
}

timing ()
{
    echo "Start $2 for $3"

    SECONDS=0
    $1
    duration=$SECONDS
    dt=$(date '+%d/%m/%Y %H:%M:%S');
    echo "$dt"
    echo "[Profiler] $2 for $3 spent ${duration} seconds"
}

#command functions

compile_target () {
    cd ${system_dir_path} || return
    eval ${compile_test_cmd}
}

retrofit_tests () {
    # Run 2 steps:
    # 1) Transform test cases to generate assertion func
    # 2) Pack back to test jar

    #0) check if already modified before

    cd ${t2c_dir} || return
    marker=${system_dir_path}"/assertpacker.mark"
    if [[ $if_ignore_marker != "-i" ]]; then
        if test -f "$marker"; then
            echo "$marker exists. Already transformed before, exit"
            echo "If you think this is a mistake, you should force to clean the marker by delete it"
            exit
        fi
    fi

    #1) run assertpack on target system
    #if empty, set to be all
    test_name=${test_name:-'all'}
    test_method=${test_method:-'all'}
    scripts/analysis/assertpack.sh "${system_classes_dir_path} ${test_classes_dir_path}" ${test_name} ${test_method} ${conf_file_realpath} ${client_main_class}

    #2) pack test classes (assert classes && modified test classes) back, two locations are needed
    #   a) test phase
    #       e.g. zookeeper/build/test/classes
    #   b) runtime phase
    #       e.g. for 3.4.11, we would pack test classes to zookeeper/build/lib
    cp -r sootOutput/org ${test_classes_dir_path}
    jar cvf sootOutput/t2c-runtime.jar sootOutput/org
    cp sootOutput/t2c-runtime.jar ${runtime_lib_path}

    #leave marker
    touch $marker
}

recover_tests () {
    cd ${system_dir_path} || return
    eval ${clean_test_cmd}

    cd ${t2c_dir} || return
    timing install "install" ""

    cd ${system_dir_path} || return
    eval ${compile_test_cmd}

    marker=${system_dir_path}"/assertpacker.mark"
    rm $marker
}

build_template () {
    cd ${system_dir_path} || return
    echo "full_class_path: ${full_class_path}"
    echo "build_template:"
    (set -x; timeout ${single_command_timeout_threshold} java -cp ${full_class_path} \
     -Xmx8g \
     -Dconf=${conf_file_realpath}  \
     -Dt2c.mode=build \
     -Dt2c.testname=${test_name} \
     -Dlog4j.configuration=${log4j_conf} \
     -Dt2c.t2c_root_abs_path=${t2c_dir} -Dt2c.target_system_abs_path=${system_dir_path} \
     -Dt2c.test_trace_prefix=${test_trace_prefix} \
     -Dt2c.ticket_id=${ticket_id} \
     -Dt2c.pindex=${pindex} \
     -Dt2c.ptotal=${ptotal} \
     ${jvm_args_for_tests} \
     edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine)

    if [ $? -eq 124 ]
    then
      echo "[ERROR] timeout, threshold is " ${single_command_timeout_threshold}
    fi
}

validate_template () {
    cd ${system_dir_path} || return
    echo "full_class_path: ${full_class_path}"
    echo "build_template:"
    (set -x; timeout ${single_command_timeout_threshold} java -cp ${full_class_path} \
     -Xmx8g \
     -Dconf=${conf_file_realpath}  \
     -Dt2c.mode=validate \
     -Dt2c.testname=${test_name} \
     -Dlog4j.configuration=${log4j_conf} \
     -Dt2c.t2c_root_abs_path=${t2c_dir} -Dt2c.target_system_abs_path=${system_dir_path} \
     -Dt2c.test_trace_prefix=${test_trace_prefix} \
     -Dt2c.ticket_id=${ticket_id} \
     -Dt2c.pindex=${pindex}\
     -Dt2c.ptotal=${ptotal}\
     ${jvm_args_for_tests} \
      edu.jhu.order.t2c.dynamicd.tscheduler.TestEngine)

    if [ $? -eq 124 ]
    then
      echo "[ERROR] timeout, threshold is " ${single_command_timeout_threshold}
    fi
}

load_templates () {
    cd ${system_dir_path} || return
    cp -r templates_out/ templates_in/
}

install () {
    echo "Run install command: ${install_lib_cmd}"
    eval ${install_lib_cmd}
}


test_runtime () {
    cd ${system_dir_path} || return
    eval ${runtime_bench_cmd}
}

test_workflow () {

    timing recover_tests "recover_tests" ""
    timing install "install" ""
    timing retrofit_tests "retrofit_tests" ${ticket_file_path}
    source ${ticket_file_path}
    timing build_template "build_template" ${ticket_file_path}
    #timing validate_template "validate_template" ${ticket_file_path}
    timing load_templates "load_templates" ${ticket_file_path}
    timing test_runtime "test_runtime" ${ticket_file_path}
}

clean_state ()
{
  cd ${system_dir_path} || return
  echo "clean states now for repo: ${system_dir_path}"

  git reset --hard > /dev/null 2>&1

  #do a check again see if it's really clean
      #we sometimes ran into unstaged changes that cannot be removed, it seems to be issues from this:
  # https://stackoverflow.com/questions/18536863/git-refuses-to-reset-discard-files
  if ! git diff-files --quiet --ignore-submodules --
  then
    echo "ran into unstaged files still, do a thorough cleanup"

    git rm --cached -r . > /dev/null 2>&1
    git reset --hard > /dev/null 2>&1
    git add . > /dev/null 2>&1
    git commit -m "[OathKeeper] Normalize line endings" > /dev/null 2>&1
  fi
}

checkout ()
{
    clean_state
    cd ${t2c_dir} || return

    #clean saved value by reloading the default setting,
    source ${conf_file_path}

    cd ${system_dir_path} || return
    git checkout -f ${commit_id}
}

banner

if [[ $1 == "help" ]]
then
    usage
    exit 0
fi

if [[ $1 == "compile" ]]
then
    echo "building the T2C tool now"
    mvn clean package -DskipTests
    exit 0
fi


conf_file_path=$2
conf_file_realpath=$(realpath ${conf_file_path})
source ${conf_file_path}

# Remove trailing slash from dir
case $system_dir_path in
    *[!/]*/) system_dir_path=${system_dir_path%"${system_dir_path##*[!/]}"};;
    *[/]) system_dir_path="/";;
esac

full_class_path=${t2c_lib}:${test_classes_dir_path}:${java_class_path}

if [[ $1 == "compile_target" ]]
then
    timing compile_target "compile_target" ${system_dir_path}
elif [[ $1 == "patch" ]]
then
    echo "install tool to target system startup scripts and classpath"
    echo "essentially 1) add jar to classpath 2) add jvm flags 3) replace main class to our wrapper"
    str="s/T2C_DIR_MACRO/${t2c_dir//\//\\/}/g; s/SYS_DIR_MACRO/${system_dir_path//\//\\/}/g; s/CONF_PATH_MACRO/${conf_file_realpath//\//\\/}/g"
    echo ${str}
    cd ${system_dir_path} || return
    if [[ $3 == "zookeeper" ]]
    then
        perl -p -e "${str}" ${patch_path} > tmp.patch
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
    elif [[ $3 == "hdfs" ]]
    then
        perl -p -e "${str}" ${patch_path} > tmp.patch
        version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
        cp hadoop-common-project/hadoop-common/src/main/bin/hadoop-functions.sh hadoop-dist/target/hadoop-${version}/libexec/
    elif [[ $3 == "cassandra" ]]
    then
        perl -p -e "${str}" ${patch_path} > tmp.patch
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
    elif [[ $3 == "hbase" ]]
    then
        perl -p -e "${str}" ${patch_path} > tmp.patch
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
    else
        echo "[ERROR] missing legal preset system name"
        return
    fi
    echo "successfully patch for target system"
elif [[ $1 == "patch_invivo_checkers" ]]
then
    echo "install invivo checkers to target system startup scripts and classpath"
    echo "essentially 1) add jar to classpath 2) add jvm flags 3) replace main class to our wrapper"
    str="s/T2C_DIR_MACRO/${t2c_dir//\//\\/}/g and s/SYS_DIR_MACRO/${system_dir_path//\//\\/}/g and s/CONF_PATH_MACRO/${conf_file_realpath//\//\\/}/g"
    echo ${str}
    cd ${system_dir_path} || return
    if [[ $3 == "zookeeper" ]]
    then
        perl -p -e "${str}" ${t2c_dir}/conf/samples/zk-patches/install_zk-3.4.11-invivo.patch > tmp.patch
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
    elif [[ $3 == "hdfs" ]]
    then
        perl -p -e "${str}" ${t2c_dir}/conf/samples/hdfs-patches/install_hdfs.patch > tmp.patch
        version=$(perl -ne 'print and last if s/.*<version>(.*)<\/version>.*/\1/;' < pom.xml)
        cat tmp.patch
        git apply tmp.patch
        rm tmp.patch
        cp hadoop-common-project/hadoop-common/src/main/bin/hadoop-functions.sh hadoop-dist/target/hadoop-${version}/libexec/
    else
        echo "[ERROR] missing legal preset system name"
        return
    fi
    echo "successfully patch for target system"
elif [[ $1 == "retrofit" ]]
then
    if_ignore_marker=$3
    timing retrofit_tests "retrofit_tests" ${ticket_file_path}
elif [[ $1 == "build" ]]
then
    pindex=$3
    ptotal=$4
    echo $pindex
    timing build_template "build_template" ${ticket_file_path}
elif [[ $1 == "parallel_build" ]]
then
    #distributed mode
    total=$(wc -l $3)
    count=0
    cat $3 | while read line || [[ -n $line ]];
    do
       ssh -n -f $line "cd ${t2c_dir} ; nohup ./run_engine.sh build $2 $count $total &> $line.build.$(date +"%Y_%m_%d_%I_%M_%p").log &"
       ((count++))
    done
    echo "on each host, you can use command to see the result: vim \$(ls -Art *.log | tail -n 1)"
elif [[ $1 == "validate" ]]
then
    pindex=$3
    ptotal=$4
    timing validate_template "validate_template" ${ticket_file_path}
elif [[ $1 == "parallel_validate" ]]
then
    #distributed mode
    total=$(wc -l $3)
    count=0
    cat $3 | while read line || [[ -n $line ]];
    do
       ssh -n -f $line "cd ${t2c_dir} ; nohup ./run_engine.sh validate $2 $count $total &> $line.validate.$(date +"%Y_%m_%d_%I_%M_%p").log &"
       ((count++))
    done
    echo "on each host, you can use command to see the result: vim \$(ls -Art *.log | tail -n 1)"
elif [[ $1 == "checkout" ]]
then
    echo "checkout the target system version after a particular fix, this is for verify use"
    checkout
elif [[ $1 == "install" ]]
then
    timing install "install" ${system_dir_path}
elif [[ $1 == "test_workflow" ]]
then
    ticket_file_path=$3
    timing test_workflow "testworkflow" ${ticket_file_path}
elif [[ $1 == "recover_tests" ]]
then
    timing recover_tests "recover_tests" ""
elif [[ $1 == "load" ]]
then
    timing load_templates "load_templates" ""
elif [[ $1 == "test_runtime" ]]
then
    timing test_runtime "test_runtime" ""
elif [[ $1 == "clean" ]]
then
    echo "clean state of target system after abortion of execution"
    clean_state
elif [[ $1 == "kill" ]]
then
    pkill -f "timeout"
    pkill -f "./run_engine.sh"
    kill -9 `jps | grep "TestEngine" | cut -d " " -f 1`
    kill -9 `jps | grep "InferEngine" | cut -d " " -f 1`
elif [[ $1 == "metrics" ]]
then
    java -cp ${full_class_path} \
      edu.jhu.order.t2c.staticd.tool.TemplateMetricsAnalyzer $3
elif [[ $1 == "validate_merge" ]]
then
    cd ${system_dir_path} || return
    java -cp ${full_class_path} \
         -Dt2c.t2c_root_abs_path=${t2c_dir} -Dt2c.target_system_abs_path=${system_dir_path} \
              -Dconf=${conf_file_realpath}  \
      edu.jhu.order.t2c.staticd.tool.TemplateValidationMerger
#commonly used to collect generated checker files from different nodes
elif [[ $1 == "sync_data" ]]
then
    mkdir sync_tmp
    cat $3 | while read line || [[ -n $line ]];
    do
       scp -r "$line:$4/*" ./sync_tmp/
    done

    cat $3 | while read line || [[ -n $line ]];
    do
       scp -r ./sync_tmp/* "$line:$4/"
    done
    rm -rf sync_tmp
#commonly used to merge file content of same name, like inv.id
elif [[ $1 == "merge_file" ]]
then
    mkdir -p merge_output
    cat $3 | while read line || [[ -n $line ]];
    do
       scp "$line:$4" ./merge_output/tmp
       fname=$(basename $4)
       cat ./merge_output/tmp >> ./merge_output/${fname}
    done
    rm ./merge_output/tmp
    echo "written in ./merge_output/${fname}"
elif [[ $1 == "inner_join" ]]
then
    java -cp ${t2c_lib} \
      edu.jhu.order.t2c.staticd.tool.TemplateListJoiner
else
    echo "Bad command, check again."
fi
