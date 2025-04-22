#run this on single node
#usage: install_ZK-1754.sh [T2C_root] [zk_root]
#cd $1
#./run_engine.sh clean conf/samples/zk-3.4.11.properties
cd $2
#git checkout master
#git checkout e3c1c87739b9fede7a0fcad0aaad0ca65b5101a9
#force cleaning
rm -f src/java/lib/ivy-2.2.0.jar  
git apply $1/expr/reproduce/zookeeper/ZK-1754/ZK-1754-3.4.11.patch
ant compile
