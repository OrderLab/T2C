#! /bin/bash
#single node should be sufficient for this case
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1
git checkout e3c1c87739b9fede7a0fcad0aaad0ca65b5101a9

cp $script_dir/zoo.cfg ./conf
echo dataDir=$1 >> ./conf/zoo.cfg

git apply ~/T2C-EvalAutomatons/zookeeper/ZK-1754/hook_ZK-1754.patch
ant
