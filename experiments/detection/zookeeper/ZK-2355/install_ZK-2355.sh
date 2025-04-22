#run this scripts on all nodes
#this script is hardcoded, need to be paramerized
cd ~/zookeeper
git checkout 69710181
git checkout -f HEAD~1
cd ~/zookeeper
git apply ~/T2C-EvalAutomatons/zookeeper/ZK-2355/hook_ZK-2355.patch
ant

