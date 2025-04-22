#single node should be sufficient for this case
cd ~/zookeeper
git checkout tags/release-3.4.11
git apply ~/T2C-EvalAutomatons/zookeeper/ZK-1208/hook_ZK-1208.patch
ant
